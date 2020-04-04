/*
 * PlanningAgent.java
 *
 * Copyright (C) 2020 Vladislav Nikolov Vasilev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://www.gnu.org/licenses/gpl-3.0.html.
 */

package controller;

import core.game.Observation;
import core.player.AbstractPlayer;
import core.game.StateObservation;
import core.vgdl.VGDLRegistry;
import org.yaml.snakeyaml.constructor.Constructor;
import tools.ElapsedCpuTimer;

import ontology.Types;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import tools.Vector2d;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

import org.yaml.snakeyaml.Yaml;

public class PlanningAgent extends AbstractPlayer {
    protected Agenda agenda;

    protected List<String> PDDLGameStatePredicates;
    protected Map<String, Set<String>> PDDLGameStateObjects;

    protected PDDLPlan PDDLPlan;
    protected Iterator<PDDLAction> iterPlan;
    protected GameInformation gameInformation;
    protected List<String> savedGoalPredicates;

    protected boolean mustReplan;

    protected Set<String> connectionSet;
    protected Map<String, Set<String>> gameElementVars;

    public PlanningAgent(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        //GameInformation a = new GameInformation("planning/prueba.yaml");
        Yaml yaml = new Yaml(new Constructor(GameInformation.class));
        try {
            InputStream inputStream = new FileInputStream(new File("games-information/boulderdash.yaml"));
            this.gameInformation = yaml.load(inputStream);
            System.out.println(this.gameInformation.domainName);
            System.out.println(this.gameInformation.gameElementsCorrespondence);
            System.out.println(this.gameInformation.avatarVariable);
        } catch (FileNotFoundException e) {
            System.out.println(e.getStackTrace());
        }

        // Initialize PDDL game state information
        this.savedGoalPredicates = new ArrayList<>();
        this.PDDLGameStatePredicates = new ArrayList<>();
        this.PDDLGameStateObjects = new HashMap<>();
        this.gameInformation.variablesTypes
                .keySet()
                .stream()
                .forEach(key -> this.PDDLGameStateObjects.put(key, new LinkedHashSet<>()));

        this.PDDLPlan = new PDDLPlan();
        this.iterPlan = PDDLPlan.iterator();

        this.agenda = new Agenda(this.gameInformation.goals);

        this.mustReplan = true;

        this.extractVariablesFromPredicates();
        System.out.println(this.gameElementVars);

        this.setConnectionSet(stateObservation);
        System.out.println(this.connectionSet);
        System.out.println(this.PDDLGameStateObjects);
    }

    private void setConnectionSet(StateObservation stateObservation) {
        // Initialize connection set
        this.connectionSet = new LinkedHashSet<>();

        // Get the observations of the game state as elements of the VGDDLRegistry
        String[][] gameMap = this.getGameElementsMatrix(stateObservation);

        final int X_MAX = gameMap.length, Y_MAX = gameMap[0].length;

        for (int y = 0; y < Y_MAX; y++) {
            for (int x = 0; x < X_MAX; x++) {
                // Create string containing the current cell
                String currentCell = String.format("%s_%d_%d", this.gameInformation.cellVariable, x, y).replace("?", "");

                if (y - 1 >= 0) {
                    String connection = this.gameInformation.connections.get(Position.UP);
                    connection = connection.replace("?c", currentCell);
                    connection = connection.replace("?p", String
                                    .format("%s_%d_%d", this.gameInformation.cellVariable, x, y - 1)
                                    .replace("?", ""));

                    this.connectionSet.add(connection);
                }

                if (y + 1 < Y_MAX) {
                    String connection = this.gameInformation.connections.get(Position.DOWN);
                    connection = connection.replace("?c", currentCell);
                    connection = connection.replace("?n", String
                            .format("%s_%d_%d", this.gameInformation.cellVariable, x, y + 1)
                            .replace("?", ""));

                    this.connectionSet.add(connection);
                }

                if (x - 1 >= 0) {
                    String connection = this.gameInformation.connections.get(Position.LEFT);
                    connection = connection.replace("?c", currentCell);
                    connection = connection.replace("?p", String
                            .format("%s_%d_%d", this.gameInformation.cellVariable, x - 1, y)
                            .replace("?", ""));

                    this.connectionSet.add(connection);
                }

                if (x + 1 < X_MAX) {
                    String connection = this.gameInformation.connections.get(Position.RIGHT);
                    connection = connection.replace("?c", currentCell);
                    connection = connection.replace("?n", String
                            .format("%s_%d_%d", this.gameInformation.cellVariable, x + 1, y)
                            .replace("?", ""));

                    this.connectionSet.add(connection);
                }
            }
        }
    }

    private void extractVariablesFromPredicates() {
        Map<String, Set<String>> varsFromPredicates = new HashMap<>();

        Pattern variablePattern = Pattern.compile("\\?[a-zA-Z]+");

        for (Map.Entry<String, ArrayList<String>> entry: this.gameInformation.gameElementsCorrespondence.entrySet()) {
            String gameObservation = entry.getKey();
            Set<String> variables = new HashSet<>();

            for (String observation: entry.getValue()) {
                Matcher variableMatcher = variablePattern.matcher(observation);
                //System.out.println(variableMatcher.find());
                while (variableMatcher.find()) {
                    for (int i = 0; i <= variableMatcher.groupCount(); i++) {
                        variables.add(variableMatcher.group(i));
                    }
                }
            }

            varsFromPredicates.put(gameObservation, variables);
        }

        this.gameElementVars = varsFromPredicates;
    }

    @Override
    public Types.ACTIONS act(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        Types.ACTIONS action;

        // Get the player's orientation
        Vector2d orientation = stateObservation.getAvatarOrientation();

        this.translateGameStateToPDDL(stateObservation);

        if (this.mustReplan) {
            System.out.println("I need to find a plan!");
            //System.out.println(Parser.<String, ArrayList<String>>parseJSONFile("correspondence.json").get("A").get(0));
            //System.out.println(VGDLRegistry.GetInstance().getRegisteredSpriteKey(10));

            long time = elapsedCpuTimer.remainingTimeMillis();
            this.agenda.setCurrentGoal();
            this.writePDDLGameStateProblem();
            System.out.println("Consumed time: " + (time - elapsedCpuTimer.remainingTimeMillis()));

            //Determine an index randomly and get the action to return.
            action = Types.ACTIONS.ACTION_NIL;

            time = elapsedCpuTimer.remainingTimeMillis();
            this.PDDLPlan = callOnlinePlanner();
            this.iterPlan = PDDLPlan.iterator();
            this.mustReplan = false;
            System.out.println("Consumed time waiting for planner's response: " + (time - elapsedCpuTimer.remainingTimeMillis()));
            //this.actionList = translateOutputPlan();
        } else {
            //System.out.println(this.actionList);
            PDDLAction nextPDDLAction = this.iterPlan.next();

            boolean satisfiedPrec = this.checkPreconditions(nextPDDLAction);


            if (satisfiedPrec) {
                System.out.println("//////////////////////////////All preconditions satisfied");
            } else {
                System.out.println("//////////////////////////////One or more preconditions hasn't been satisfied. ERROR.");
                this.agenda.haltCurrentGoal();
                System.out.println(this.agenda);
                this.mustReplan = true;
            }

            action = nextPDDLAction.getGVGAIAction();

            if (!this.iterPlan.hasNext()) {
                if (this.agenda.getCurrentGoal().isSaveGoal()) {
                    this.savedGoalPredicates.add(this.agenda.getCurrentGoal().getGoalPredicate());
                }
                this.agenda.updateReachedGoals();
                System.out.println(this.agenda);
                this.mustReplan = true;
            }
            /*
            action = this.iterPlan.next().getGVGAIAction();

            if (!this.iterPlan.hasNext()) {
                this.agenda.removeFirst();
            }*/
        }

        //Return the action.
        return action;
    }

    public boolean checkPreconditions(PDDLAction PDDLAction) {
        boolean satisfiedPreconditions = true;
        System.out.println(PDDLAction.getPreconditions());

        // Check whether all preconditions are satisfied
        for (String precondition: PDDLAction.getPreconditions()) {
            // If the precondition is negative, it has to be checked that the positive can't be found
            if (precondition.contains("not")) {
                String positivePred = precondition.replace("(not ", "");
                positivePred = positivePred.substring(0, positivePred.length() - 1);

                if (this.PDDLGameStatePredicates.contains(positivePred)) {
                    System.out.println(precondition);
                    satisfiedPreconditions = false;
                }
            } else {
                if (!this.PDDLGameStatePredicates.contains(precondition)) {
                    System.out.println(precondition);
                    satisfiedPreconditions = false;
                }
            }
        }

        return satisfiedPreconditions;
    }

    public void callPlanner() {
        // Strings that containt the paths for the planner, the domain file,
        // the problem file and the log file
        String plannerRoute = "planning/ff",
                domainFile = "planning/domain.pddl",
                problemFile = "planning/problem.pddl",
                logFileRoute = "planning/plan.txt";

        // Create new process which will run the planner
        ProcessBuilder pb = new ProcessBuilder(plannerRoute, "-o", domainFile,
                "-f", problemFile, "-O", "-g", "1", "-h", "1");
        File log = new File(logFileRoute);

        // Clear log file
        try {
            PrintWriter writer = new PrintWriter(log);
            writer.print("");
            writer.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Error: archivo no encontrado " + ex);
        }


        // Redirect error and output streams
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));

        // Run process and wait until it finishes
        try {
            Process process = pb.start();
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long a = System.currentTimeMillis();
        callOnlinePlanner();
        long b = System.currentTimeMillis() - a;
        System.out.println("###############################" + b);
    }

    public PDDLPlan callOnlinePlanner() {
        // Read domain and problem files
        String domain = readFile(this.gameInformation.domainFile),
               problem = readFile("planning/problem.pddl");

        // Call online planner and get its response
        HttpResponse<JsonNode> response = Unirest.post("http://solver.planning.domains/solve")
                .header("accept", "application/json")
                .field("domain", domain)
                .field("problem", problem)
                .asJson();

        // Here the response should be checked in case there's been an error

        // Create a new PDDLPlan instance if a valid plan has been found
        PDDLPlan PDDLPlan = new PDDLPlan(response.getBody().getObject(), this.gameInformation.actionsCorrespondence);

        return PDDLPlan;
    }

    private String readFile(String filename) {
        // Create builder that will contain the file's content
        StringBuilder contentBuilder = new StringBuilder();

        // Get content from file line per line
        try (Stream<String> stream = Files.lines(Paths.get(filename))) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contentBuilder.toString();
    }

    /*
    public List<Types.ACTIONS> translateOutputPlan() {
        // ArrayList of actions
        List<Types.ACTIONS> actions = new ArrayList<>();

        // Plan file
        File planFile = new File("planning/plan.txt");

        try {
            BufferedReader br = new BufferedReader(new FileReader(planFile));

            String line;

            while ((line = br.readLine()) != null) {
                String upperLine = line.toUpperCase();

                for (String action: this.actionCorrespondence.keySet()) {
                    if (upperLine.contains(action)) {
                        actions.add(this.actionCorrespondence.get(action));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return actions;
    }
    */

    public void translateGameStateToPDDL(StateObservation stateObservation) {
        // Get the observations of the game state as elements of the VGDDLRegistry
        String[][] gameMap = this.getGameElementsMatrix(stateObservation);

        // Clear the list of predicates and objects
        this.PDDLGameStatePredicates.clear();
        this.PDDLGameStateObjects.values().stream().forEach(val -> val.clear());

        final int X_MAX = gameMap.length, Y_MAX = gameMap[0].length;

        for (int y = 0; y < Y_MAX; y++) {
            for (int x = 0; x < X_MAX; x++) {
                // Get the observation in the current cell
                String cellObservation = gameMap[x][y];

                // If the observation is in the domain, instantiate its predicates
                if (this.gameInformation.gameElementsCorrespondence.containsKey(cellObservation)) {
                    List<String> predicateList = this.gameInformation.gameElementsCorrespondence.get(cellObservation);

                    // Instantiate each predicate
                    for (String predicate: predicateList) {
                        String predicateInstance = predicate;

                        // Iterate over all the variables associated to the game element and
                        // instantiate those who appear in the predicate
                        for (String variable: this.gameElementVars.get(cellObservation)) {
                            if (predicate.contains(variable)) {
                                String variableInstance;

                                if (variable.equals(this.gameInformation.avatarVariable)) {
                                    variableInstance = variable.replace("?", "");

                                    // If orientations are being used, add predicate associated
                                    // to the player's orientation
                                    if (this.gameInformation.orientationCorrespondence != null) {
                                        Vector2d avatarOrientation = stateObservation.getAvatarOrientation();
                                        Position orientation = null;

                                        if (avatarOrientation.x == 1.0) {
                                            orientation = Position.RIGHT;
                                        } else if (avatarOrientation.x == -1.0) {
                                            orientation = Position.LEFT;
                                        } else if (avatarOrientation.y == 1.0) {
                                            orientation = Position.DOWN;
                                        } else if (avatarOrientation.y == -1.0) {
                                            orientation = Position.UP;
                                        }

                                        this.PDDLGameStatePredicates.add(this.gameInformation.orientationCorrespondence
                                                .get(orientation)
                                                .replace(variable, variableInstance));
                                    }
                                } else {
                                    variableInstance = String.format("%s_%d_%d", variable, x, y).replace("?", "");
                                    if (cellObservation.equals("floor")) {
                                        System.out.println(variableInstance);
                                    }
                                }

                                // Add instantiated variables to the predicate
                                predicateInstance = predicateInstance.replace(variable, variableInstance);

                                // Save instantiated variable
                                this.PDDLGameStateObjects.get(variable).add(variableInstance);
                            }
                        }

                        // Save instantiated predicate
                        this.PDDLGameStatePredicates.add(predicateInstance);
                    }
                }
            }
        }

        // Add connections to predicates
        this.connectionSet.stream().forEach(connection -> this.PDDLGameStatePredicates.add(connection));

        // Add saved goals
        this.savedGoalPredicates.stream().forEach(goal -> this.PDDLGameStatePredicates.add(goal));
    }

    public String[][] getGameElementsMatrix(StateObservation so) {

        // Get the current game state
        ArrayList<Observation>[][] gameState = so.getObservationGrid();

        // Get the number of X tiles and Y tiles
        final int X_MAX = gameState.length, Y_MAX = gameState[0].length;

        // Create a new matrix, representing the game's map
        String[][] gameStringMap = new String[X_MAX][Y_MAX];

        /*
         * Iterate over the map and transform it to string
         * The VGDLRegistry contains information that will allow us to transform
         * the current StateObservation to a matrix
         */
        for (int y = 0; y < Y_MAX; y++) {
            for (int x = 0; x < X_MAX; x++) {
                /*
                 * If there's an observation, get its name using the itype
                 * information
                 */
                if (gameState[x][y].size() > 0) {
                    int itype = gameState[x][y].get(0).itype;
                    gameStringMap[x][y] = VGDLRegistry.GetInstance().getRegisteredSpriteKey(itype);
                } else {
                    gameStringMap[x][y] = "background";
                }
            }
        }


        /*for (int y = 0; y < Y_MAX; y++) {
            for (int x = 0; x < X_MAX; x++) {
                System.out.print(gameStringMap[x][y] + " ");
            }
            System.out.println();
        }*/

        return gameStringMap;
    }

    private void writePDDLGameStateProblem() {
        String outGoal = this.agenda.getCurrentGoal().getGoalPredicate();

        try (BufferedWriter bf = new BufferedWriter(new FileWriter("planning/problem.pddl"))) {
            // Write problem name
            // THIS LINE HAS TO BE CHANGED LATER ON, ALLOWING TO CHANGE THE PROBLEM
            bf.write("(define (problem Boulders)");
            bf.newLine();

            // Write domain that is used
            // THIS LINE HAS TO BE CHANGED LATER ON, ALLOWING AUTOMATIC CHANGE
            bf.write(String.format("(:domain %s)", this.gameInformation.domainName));
            bf.newLine();

            // Write the objects
            // Each variable will be written
            bf.write("(:objects");
            bf.newLine();

            // Write each object
            for (String key: this.PDDLGameStateObjects.keySet()) {
                if (!this.PDDLGameStateObjects.get(key).isEmpty()) {
                    String objectsStr = String.join(" ", this.PDDLGameStateObjects.get(key));
                    objectsStr += String.format(" - %s", this.gameInformation.variablesTypes.get(key));
                    bf.write(objectsStr);
                    bf.newLine();
                }
            }

            // Finish object writing
            bf.write(")");
            bf.newLine();

            // Start init writing
            bf.write("(:init");
            bf.newLine();

            // Write the predicates list into the file
            for (String predicate: this.PDDLGameStatePredicates) {
                bf.write(predicate);
                bf.newLine();
            }

            // Finish init writing
            bf.write(")");
            bf.newLine();

            // Write goal
            // THIS HAS TO CHANGE
            bf.write("(:goal");
            bf.newLine();

            bf.write("(AND");
            bf.newLine();

            bf.write(outGoal);
            bf.newLine();

            bf.write(")");
            bf.newLine();

            bf.write(")");
            bf.newLine();

            // Finish problem writing
            bf.write(")");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
