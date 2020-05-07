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
import kong.unirest.json.JSONObject;
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
    // The following attributes can be modified
    protected final static String GAME_PATH = "game-config-files/boulderdash.yaml";
    protected final static boolean DEBUG_MODE_ENABLED = true;

    // Agenda that contains preempted, current and reached goals
    protected Agenda agenda;

    // PDDL predicates and objects
    protected List<String> PDDLGameStatePredicates;
    protected Map<String, Set<String>> PDDLGameStateObjects;

    // Plan to the current goal and iterator to iterate over it
    protected PDDLPlan PDDLPlan;
    protected Iterator<PDDLAction> iterPlan;
    
    // Game information data structure (loaded from a .yaml file) and file path
    protected GameInformation gameInformation;
    
    // List of reached goal predicates that have to be saved
    protected List<String> reachedSavedGoalPredicates;

    // Variable that indicates whether the agent has to find a new plan or not
    protected boolean mustPlan;

    // Set of connections between cells
    protected Set<String> connectionSet;
    protected Map<String, Set<String>> gameElementVars;

    protected int turn;

    public PlanningAgent(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        Yaml yaml = new Yaml(new Constructor(GameInformation.class));
        try {
            InputStream inputStream = new FileInputStream(new File(PlanningAgent.GAME_PATH));
            this.gameInformation = yaml.load(inputStream);
        } catch (FileNotFoundException e) {
            System.out.println(e.getStackTrace());
        }

        // Initialize PDDL game state information
        this.reachedSavedGoalPredicates = new ArrayList<>();
        this.PDDLGameStatePredicates = new ArrayList<>();
        this.PDDLGameStateObjects = new HashMap<>();
        this.gameInformation.variablesTypes
                .keySet()
                .stream()
                .forEach(key -> this.PDDLGameStateObjects.put(key, new LinkedHashSet<>()));

        this.PDDLPlan = new PDDLPlan();
        this.iterPlan = PDDLPlan.iterator();

        this.agenda = new Agenda(this.gameInformation.goals);

        this.mustPlan = true;

        this.extractVariablesFromPredicates();
        //System.out.println(this.gameElementVars);

        this.setConnectionSet(stateObservation);
        //System.out.println(this.connectionSet);
        //System.out.println(this.PDDLGameStateObjects);
        this.turn = -1;
    }

    private void setConnectionSet(StateObservation stateObservation) {
        // Initialize connection set
        this.connectionSet = new LinkedHashSet<>();

        // Get the observations of the game state as elements of the VGDDLRegistry
        HashSet<String>[][] gameMap = this.getGameElementsMatrix(stateObservation, false);

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

    private void displayInformation(String[] messages) {
        // Show messages
        this.printMessages(messages);

        // Request input
        Scanner scanner = new Scanner(System.in);
        int option = 0;
        final int EXIT_OPTION = 3;

        while (option != EXIT_OPTION) {
            System.out.println("\nSelect what information you want to display:");
            System.out.println("[1] : Agenda");
            System.out.println("[2] : Current plan");
            System.out.println("[3] : Continue execution");
            System.out.print("\n$ ");

            // Ignore option if it's not an integer
            while (!scanner.hasNextInt()) {
                scanner.next();
                System.out.print("\n$ ");
            }

            option = scanner.nextInt();

            switch (option) {
                case 1:
                    System.out.println(this.agenda);
                    break;
                case 2:
                    System.out.println(this.PDDLPlan);
                    break;
                case EXIT_OPTION:
                    break;
                default:
                    System.out.println("Incorrect option!");
                    break;
            }
        }
    }

    private void showMessagesWait(String[] messages) {
        // Show messages
        this.printMessages(messages);

        Scanner scanner = new Scanner(System.in);

        System.out.println("Press [ENTER] to continue");
        scanner.nextLine();
    }

    private void printMessages(String[] messages) {
        for (String m: messages) {
            System.out.println(m);
        }
    }

    @Override
    public Types.ACTIONS act(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        Types.ACTIONS action = Types.ACTIONS.ACTION_NIL;
        this.turn++;

        // SHOW DEBUG INFORMATION
        if (PlanningAgent.DEBUG_MODE_ENABLED) {
            System.out.println(String.format("\n ---------- Turn %d ----------\n", this.turn));
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Translate game state to PDDL predicates
        this.translateGameStateToPDDL(stateObservation);

        // If there's no plan, spend one turn searching for one
        if (this.mustPlan) {
            // Set current goal
            this.agenda.setCurrentGoal();

            // SHOW DEBUG INFORMATION
            if (PlanningAgent.DEBUG_MODE_ENABLED) {
                this.displayInformation(new String[]{"I don't have a plan to the current goal or I must replan!"});
            }

            // Write PDDL predicates into the problem file
            this.writePDDLGameStateProblem();

            this.PDDLPlan = callOnlinePlanner();
            this.iterPlan = PDDLPlan.iterator();
            this.mustPlan = false;

            // SHOW DEBUG INFORMATION
            if (PlanningAgent.DEBUG_MODE_ENABLED) {
                this.displayInformation(new String[]{"Translated output plan"});
            }
        } else {
            // Check preconditions for next action
            PDDLAction nextPDDLAction = this.iterPlan.next();

            // SHOW DEBUG INFORMATION
            if (PlanningAgent.DEBUG_MODE_ENABLED) {
                System.out.println("The agent will try to execute the following action:" + nextPDDLAction.toString());
                System.out.println("\nChecking preconditions...");

                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            boolean satisfiedPreconditions = this.checkPreconditions(nextPDDLAction.getPreconditions(), PlanningAgent.DEBUG_MODE_ENABLED);

            if (satisfiedPreconditions) {
                // SHOW DEBUG INFORMATION
                if (PlanningAgent.DEBUG_MODE_ENABLED) {
                    System.out.println("All preconditions satisfied!");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // SHOW DEBUG INFORMATION
                if (PlanningAgent.DEBUG_MODE_ENABLED) {
                    System.out.println("\nChecking effects...");

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Check action effects
                boolean modifiedAgenda = this.checkEffects(nextPDDLAction.getEffects());

                // SHOW DEBUG INFORMATION
                if (PlanningAgent.DEBUG_MODE_ENABLED && modifiedAgenda) {
                    this.displayInformation(new String[]{
                            "\nThe agenda has been updated!"
                    });
                } else if (PlanningAgent.DEBUG_MODE_ENABLED && !modifiedAgenda) {
                    System.out.println("No goal has been reached beforehand!\n");
                }

                action = nextPDDLAction.getGVGAIAction();

                // If no actions are left, that means that the current goal has been reached
                if (!this.iterPlan.hasNext()) {
                    // SHOW DEBUG INFORMATION
                    if (PlanningAgent.DEBUG_MODE_ENABLED) {
                        this.showMessagesWait(new String[]{
                                String.format("The following goal is going the be reached after executing the next action: %s", this.agenda.getCurrentGoal()),
                                "\nIn the next turn I am going to search for a new plan!"
                        });
                    }
                    // Save the reached goal in case it has to be saved
                    if (this.agenda.getCurrentGoal().isSaveGoal()) {
                        this.reachedSavedGoalPredicates.add(this.agenda.getCurrentGoal().getGoalPredicate());
                    }

                    // Remove other reached goals if the current reached goal needs to do it
                    if (this.agenda.getCurrentGoal().getRemoveReachedGoalsList() != null) {
                        for (String reachedGoal: this.agenda.getCurrentGoal().getRemoveReachedGoalsList()) {
                            this.reachedSavedGoalPredicates.remove(reachedGoal);
                        }
                    }

                    // Update reached goals
                    this.agenda.updateReachedGoals();
                    this.mustPlan = true;
                    this.PDDLPlan.getPDDLActions().clear();

                    // SHOW DEBUG INFORMATION
                    if (PlanningAgent.DEBUG_MODE_ENABLED) {
                        this.displayInformation(new String[]{"\nThe agenda has been updated!"});
                    }
                }
            } else {
                // SHOW DEBUG INFORMATION
                if (PlanningAgent.DEBUG_MODE_ENABLED) {
                    this.showMessagesWait(new String[]{
                            "One or more preconditions couldn't be satisfied",
                            "The following goal is going to be halted:"+ this.agenda.getCurrentGoal(),
                            "\nI am going to select a new goal and find a plan to it in the following turn!"
                    });
                }

                this.agenda.haltCurrentGoal();
                this.mustPlan = true;
                this.PDDLPlan.getPDDLActions().clear();

                // SHOW DEBUG INFORMATION
                if (PlanningAgent.DEBUG_MODE_ENABLED) {
                    this.displayInformation(new String[]{"\nThe agenda has been updated!"});
                }
            }
        }

        // SHOW DEBUG INFORMATION
        if (PlanningAgent.DEBUG_MODE_ENABLED) {
            System.out.println("The following action is going to be executed in this turn: " + action);

            try {
                Thread.sleep(2250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //Return the action.
        return action;
    }

    public boolean checkPreconditions(List<String> preconditions, boolean showInformation) {
        boolean satisfiedPreconditions = true;
        List<String> falsePreconditions = new ArrayList<>();

        // Check whether all preconditions are satisfied
        for (String precondition: preconditions) {
            // If the precondition is negative, it has to be checked that the positive can't be found
            if (precondition.contains("not")) {
                String positivePred = precondition.replace("(not ", "");
                positivePred = positivePred.substring(0, positivePred.length() - 1);

                if (this.PDDLGameStatePredicates.contains(positivePred)) {
                    falsePreconditions.add(precondition);
                    satisfiedPreconditions = false;
                }
            } else {
                if (!this.PDDLGameStatePredicates.contains(precondition)) {
                    falsePreconditions.add(precondition);
                    satisfiedPreconditions = false;
                }
            }
        }

        // SHOW DEBUG INFORMATION
        if (!falsePreconditions.isEmpty() && showInformation) {
            falsePreconditions.add(0, "\nOne or more preconditions haven't been met:");
            falsePreconditions.add("\n");
            this.showMessagesWait(falsePreconditions.toArray(new String[0]));
        }

        return satisfiedPreconditions;
    }

    private PDDLSingleGoal checkSingleEffect(String predicate) {
        PDDLSingleGoal modifiedGoal = null;
        PDDLSingleGoal pendingGoal = agenda.containedPredicateInPendingGoals(predicate);
        PDDLSingleGoal preemptedGoal = agenda.containedPredicateInPreemptedGoals(predicate);

        if (pendingGoal != null) {
            agenda.setReachedFromPending(pendingGoal);
            modifiedGoal = pendingGoal;
        } else if (preemptedGoal != null) {
            agenda.setReachedFromPreempted(preemptedGoal);
            modifiedGoal = preemptedGoal;
        }

        return modifiedGoal;
    }

    public boolean checkEffects(List<PDDLAction.Effect> effects) {
        List<PDDLSingleGoal> modifiedGoals = new ArrayList<>();

        // Check not planned goals
        for (PDDLAction.Effect effect: effects) {
            if (effect.getConditions().isEmpty()) {
                // Check both lists and update them acorrdingly
                PDDLSingleGoal modifiedGoal = this.checkSingleEffect(effect.getEffectPredicate());

                if (modifiedGoal != null) {
                    modifiedGoals.add(modifiedGoal);
                }
            } else {
                boolean conditionsSatisfied = this.checkPreconditions(effect.getConditions(), false);

                if (conditionsSatisfied) {
                    PDDLSingleGoal modifiedGoal = this.checkSingleEffect(effect.getEffectPredicate());

                    if (modifiedGoal != null) {
                        modifiedGoals.add(modifiedGoal);
                    }
                }
            }
        }

        boolean modifiedAgenda = !modifiedGoals.isEmpty();

        // SHOW DEBUG INFORMATION
        if (PlanningAgent.DEBUG_MODE_ENABLED && modifiedAgenda) {
            StringBuilder builder = new StringBuilder();
            modifiedGoals.stream().forEach(goal -> builder.append(goal.toString()));
            builder.append("\n");

            this.showMessagesWait(new String[]{
                    "The following goals have been reached beforehand:",
                    builder.toString()
            });
        }

        return modifiedAgenda;
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

    public PDDLPlan callOnlinePlanner() throws PlanNotFoundException {
        // Read domain and problem files
        String domain = readFile(this.gameInformation.domainFile),
               problem = readFile("planning/problem.pddl");

        // Call online planner and get its response
        HttpResponse<JsonNode> response = Unirest.post("http://solver.planning.domains/solve")
                .header("accept", "application/json")
                .field("domain", domain)
                .field("problem", problem)
                .asJson();

        // Get the JSON from the body of the HTTP response
        JSONObject responseBody =  response.getBody().getObject();

        // SHOW DEBUG INFORMATION
        if (PlanningAgent.DEBUG_MODE_ENABLED) {
            this.showMessagesWait(new String[]{"--- Planner response ---",
                    String.format("Response status: %s", responseBody.getString("status")),
                    String.format("Result:\n%s", responseBody.getJSONObject("result").getString("output"))
            });
        }

        // Throw exception if the status is not ok
        if (!responseBody.getString("status").equals("ok")) {
            throw new PlanNotFoundException(responseBody.getJSONObject("result").getString("output"));
        }

        // Create a new PDDLPlan instance if a valid plan has been found
        PDDLPlan PDDLPlan = new PDDLPlan(responseBody, this.gameInformation.actionsCorrespondence);

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
        HashSet<String>[][] gameMap = this.getGameElementsMatrix(stateObservation, false);

        // Clear the list of predicates and objects
        this.PDDLGameStatePredicates.clear();
        this.PDDLGameStateObjects.values().stream().forEach(val -> val.clear());

        final int X_MAX = gameMap.length, Y_MAX = gameMap[0].length;

        for (int y = 0; y < Y_MAX; y++) {
            for (int x = 0; x < X_MAX; x++) {
                for (String cellObservation: gameMap[x][y]) {

                    // If the observation is in the domain, instantiate its predicates
                    if (this.gameInformation.gameElementsCorrespondence.containsKey(cellObservation)) {
                        List<String> predicateList = this.gameInformation.gameElementsCorrespondence.get(cellObservation);

                        // Instantiate each predicate
                        for (String predicate : predicateList) {
                            String predicateInstance = predicate;

                            // Iterate over all the variables associated to the game element and
                            // instantiate those who appear in the predicate
                            for (String variable : this.gameElementVars.get(cellObservation)) {
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
        }

        // Add connections to predicates
        this.connectionSet.stream().forEach(connection -> this.PDDLGameStatePredicates.add(connection));

        // Add saved goals
        this.reachedSavedGoalPredicates.stream().forEach(goal -> this.PDDLGameStatePredicates.add(goal));
    }

    public HashSet<String>[][] getGameElementsMatrix(StateObservation so, boolean debug) {
        // Get the current game state
        ArrayList<Observation>[][] gameState = so.getObservationGrid();

        // Get the number of X tiles and Y tiles
        final int X_MAX = gameState.length, Y_MAX = gameState[0].length;

        // Create a new matrix, representing the game's map
        HashSet<String>[][] gameStringMap = new HashSet[X_MAX][Y_MAX];

        /*
         * Iterate over the map and transform the observations in a [x, y] cell
         * to a HashSet of Strings. In case there's no observation, add a
         * "background" string. The VGDLRegistry contains the needed information
         * to transform the StateObservation to a matrix of sets of Strings.
         */
        for (int y = 0; y < Y_MAX; y++) {
            for (int x = 0; x < X_MAX; x++) {
                gameStringMap[x][y] = new HashSet<>();
                
                if (gameState[x][y].size() > 0) {
                    for (int i = 0; i < gameState[x][y].size(); i++) {
                        int itype = gameState[x][y].get(i).itype;
                        gameStringMap[x][y].add(VGDLRegistry.GetInstance().getRegisteredSpriteKey(itype));
                    }
                } else {
                    gameStringMap[x][y].add("background");
                }
            }
        }

        // Show map in case it has to be debugged
        if (debug) {
            for (int y = 0; y < Y_MAX; y++) {
                for (int x = 0; x < X_MAX; x++) {
                    System.out.print(gameStringMap[x][y] + " ");
                }
                System.out.println();
            }
        }

        return gameStringMap;
    }

    private void writePDDLGameStateProblem() {
        String outGoal = this.agenda.getCurrentGoal().getGoalPredicate();

        try (BufferedWriter bf = new BufferedWriter(new FileWriter("planning/problem.pddl"))) {
            // Write problem name
            bf.write(String.format("(define (problem %sProblem)", this.gameInformation.domainName));
            bf.newLine();

            // Write domain that is used
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
