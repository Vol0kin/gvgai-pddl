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

/**
 * Package that contains the planning agent along with its data structures.
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

/**
 * Planning agent class. It represents an agent which uses a planner to reach
 * a set of goals. See {@link Agenda} to find out how goals are structured.
 *
 * @author Vladislav Nikolov Vasilev
 */
public class PlanningAgent extends AbstractPlayer {
    // The following attributes can be modified
    protected static String GAME_CONFIG_FILE;
    protected static boolean DEBUG_MODE;

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

    /**
     * Class constructor. Creates a new planning agent.
     * @param stateObservation State observation of the game.
     * @param elapsedCpuTimer Elapsed CPU time.
     */
    public PlanningAgent(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        // Load game information
        Yaml yaml = new Yaml(new Constructor(GameInformation.class));

        try {
            InputStream inputStream = new FileInputStream(new File(PlanningAgent.GAME_CONFIG_FILE));
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
        this.gameElementVars = this.extractVariablesFromPredicates();
        this.connectionSet = this.generateConnectionPredicates(stateObservation);

        // Initialize plan and iterator
        this.PDDLPlan = new PDDLPlan();
        this.iterPlan = PDDLPlan.iterator();

        // Initialize agenda
        this.agenda = new Agenda(this.gameInformation.goals);

        // Set plan variable and turn
        this.mustPlan = true;
        this.turn = -1;
    }

    /**
     * Method called in each turn that returns the next action that the agent
     * will execute. It is responsible for controlling the agent's behaviour.
     * @param stateObservation State observation of the game.
     * @param elapsedCpuTimer Elapsed CPU time
     * @return Returns the action that will be executed by the agent in the
     * current turn.
     */
    @Override
    public Types.ACTIONS act(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        Types.ACTIONS action = Types.ACTIONS.ACTION_NIL;
        this.turn++;

        // SHOW DEBUG INFORMATION
        if (PlanningAgent.DEBUG_MODE) {
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
            if (PlanningAgent.DEBUG_MODE) {
                this.displayDebugInformation(new String[]{"I don't have a plan to the current goal or I must replan!"});
            }

            // Write PDDL predicates into the problem file
            this.createProblemFile();

            this.PDDLPlan = findPlan();
            this.iterPlan = PDDLPlan.iterator();
            this.mustPlan = false;

            // SHOW DEBUG INFORMATION
            if (PlanningAgent.DEBUG_MODE) {
                this.displayDebugInformation(new String[]{"Translated output plan"});
            }
        } else {
            // Check preconditions for next action
            PDDLAction nextPDDLAction = this.iterPlan.next();

            // SHOW DEBUG INFORMATION
            if (PlanningAgent.DEBUG_MODE) {
                System.out.println("The agent will try to execute the following action:" + nextPDDLAction.toString());
                System.out.println("\nChecking preconditions...");

                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            boolean satisfiedPreconditions = this.checkPreconditions(nextPDDLAction.getPreconditions(), PlanningAgent.DEBUG_MODE);

            if (satisfiedPreconditions) {
                // SHOW DEBUG INFORMATION
                if (PlanningAgent.DEBUG_MODE) {
                    System.out.println("All preconditions satisfied!");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // SHOW DEBUG INFORMATION
                if (PlanningAgent.DEBUG_MODE) {
                    System.out.println("\nChecking effects...");

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Check action effects
                boolean modifiedAgenda = this.checkEarlyReachedGoals(nextPDDLAction.getEffects());

                // SHOW DEBUG INFORMATION
                if (PlanningAgent.DEBUG_MODE && modifiedAgenda) {
                    this.displayDebugInformation(new String[]{
                            "\nThe agenda has been updated!"
                    });
                } else if (PlanningAgent.DEBUG_MODE && !modifiedAgenda) {
                    System.out.println("No goal has been reached beforehand!\n");
                }

                action = nextPDDLAction.getGVGAIAction();

                // If no actions are left, that means that the current goal has been reached
                if (!this.iterPlan.hasNext()) {
                    // SHOW DEBUG INFORMATION
                    if (PlanningAgent.DEBUG_MODE) {
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
                    if (PlanningAgent.DEBUG_MODE) {
                        this.displayDebugInformation(new String[]{"\nThe agenda has been updated!"});
                    }
                }
            } else {
                // SHOW DEBUG INFORMATION
                if (PlanningAgent.DEBUG_MODE) {
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
                if (PlanningAgent.DEBUG_MODE) {
                    this.displayDebugInformation(new String[]{"\nThe agenda has been updated!"});
                }
            }
        }

        // SHOW DEBUG INFORMATION
        if (PlanningAgent.DEBUG_MODE) {
            System.out.println("The following action is going to be executed in this turn: " + action);

            try {
                Thread.sleep(2250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return action;
    }

    /**
     * Method that checks whether the preconditions of an action are satisfied or
     * not. In this case, the preconditions are going to be satisfied if all the
     * positive preconditions are contained in the list of PDDL predicates and
     * all the negative ones aren't contained.
     * @param preconditions List of preconditions to check.
     * @param showInformation Boolean telling whether to show or not debug information.
     * @return Returns true if all preconditions are satisfied and false otherwise.
     */
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

    /**
     * Method that checks whether a goal is reached beforehand by checking
     * the effects of an action. In case some goal is reached beforehand, the
     * agenda is updated accordingly.
     * updates the agenda accordingly
     * @param effects List that contains the effect predicates of an action that
     *                will be checked.
     * @return Returns true if some goal has been reached beforehand and false
     * otherwise.
     */
    public boolean checkEarlyReachedGoals(List<PDDLAction.Effect> effects) {
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
        if (PlanningAgent.DEBUG_MODE && modifiedAgenda) {
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

    /**
     * Method that allows the agent to find a plan to the current given goal. It calls
     * the planner and translates its output, generating in the process a new PDDLPlan
     * instance.
     * @return Returns a new PDDLPlan instance.
     * @throws PlannerException Thrown when the planner's response status is not OK.
     */
    public PDDLPlan findPlan() throws PlannerException {
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
        if (PlanningAgent.DEBUG_MODE) {
            this.showMessagesWait(new String[]{"--- Planner response ---",
                    String.format("Response status: %s", responseBody.getString("status")),
                    String.format("Result:\n%s", responseBody.getJSONObject("result").getString("output"))
            });
        }

        // Throw exception if the status is not ok
        if (!responseBody.getString("status").equals("ok")) {
            throw new PlannerException(responseBody.getJSONObject("result").getString("output"));
        }

        // Create a new PDDLPlan instance if a valid plan has been found
        PDDLPlan PDDLPlan = new PDDLPlan(responseBody, this.gameInformation.actionsCorrespondence);

        return PDDLPlan;
    }

    /**
     * Method that translates a game state observation to PDDL predicates.
     * @param stateObservation State observation of the game.
     */
    public void translateGameStateToPDDL(StateObservation stateObservation) {
        // Get the observations of the game state as elements of the VGDDLRegistry
        HashSet<String>[][] gameMap = this.getGameElementsMatrix(stateObservation);

        // Clear the list of predicates and objects
        this.PDDLGameStatePredicates.clear();
        this.PDDLGameStateObjects.values().stream().forEach(val -> val.clear());

        final int X_MAX = gameMap.length, Y_MAX = gameMap[0].length;

        // Process game elements
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

    /**
     * Method that translates a game state observation to a matrix of strings which
     * represent the elements of the game in each position according to the VGDDL
     * registry. There can be more than one game element in each position.
     * @param stateObservation State observation of the game.
     * @return Returns a matrix containing the elements of the game in each position.
     */
    public HashSet<String>[][] getGameElementsMatrix(StateObservation stateObservation) {
        // Get the current game state
        ArrayList<Observation>[][] gameState = stateObservation.getObservationGrid();

        // Get the number of X tiles and Y tiles
        final int X_MAX = gameState.length, Y_MAX = gameState[0].length;

        // Create a new matrix, representing the game's map
        HashSet<String>[][] gameStringMap = new HashSet[X_MAX][Y_MAX];

        // Iterate over the map and transform the observations in a [x, y] cell
        // to a HashSet of Strings. In case there's no observation, add a
        // "background" string. The VGDLRegistry contains the needed information
        // to transform the StateObservation to a matrix of sets of Strings.
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

        return gameStringMap;
    }

    public static void setGameConfigFile(String path) {
        PlanningAgent.GAME_CONFIG_FILE = path;
    }

    public static void setDebugMode(boolean debugMode) {
        PlanningAgent.DEBUG_MODE = debugMode;
    }

    /**
     * Method that generates the connection predicates between the cells of the
     * map.
     * @param stateObservation State observation of the game.
     * @return Returns a set which preserves insertion order and contains
     * the PDDL predicates associated to the cells connections.
     */
    private Set<String> generateConnectionPredicates(StateObservation stateObservation) {
        // Initialize connection set
        Set<String> connections = new LinkedHashSet<>();

        // Get the observations of the game state as elements of the VGDDLRegistry
        HashSet<String>[][] gameMap = this.getGameElementsMatrix(stateObservation);

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

                    connections.add(connection);
                }

                if (y + 1 < Y_MAX) {
                    String connection = this.gameInformation.connections.get(Position.DOWN);
                    connection = connection.replace("?c", currentCell);
                    connection = connection.replace("?n", String
                            .format("%s_%d_%d", this.gameInformation.cellVariable, x, y + 1)
                            .replace("?", ""));

                    connections.add(connection);
                }

                if (x - 1 >= 0) {
                    String connection = this.gameInformation.connections.get(Position.LEFT);
                    connection = connection.replace("?c", currentCell);
                    connection = connection.replace("?p", String
                            .format("%s_%d_%d", this.gameInformation.cellVariable, x - 1, y)
                            .replace("?", ""));

                    connections.add(connection);
                }

                if (x + 1 < X_MAX) {
                    String connection = this.gameInformation.connections.get(Position.RIGHT);
                    connection = connection.replace("?c", currentCell);
                    connection = connection.replace("?n", String
                            .format("%s_%d_%d", this.gameInformation.cellVariable, x + 1, y)
                            .replace("?", ""));

                    connections.add(connection);
                }
            }
        }

        return connections;
    }

    /**
     * Method used to extract the variables from the predicates associated to a game
     * element and associate them to game elements directly.
     * @return Returns a mapping between the game elements and the variables
     * associated to them.
     */
    private Map<String, Set<String>> extractVariablesFromPredicates() {
        Map<String, Set<String>> varsFromPredicates = new HashMap<>();

        // Pattern that matches a variable
        Pattern variablePattern = Pattern.compile("\\?[a-zA-Z]+");

        // Iterate over all the pairs <game element: [predicates]>
        for (Map.Entry<String, ArrayList<String>> entry: this.gameInformation.gameElementsCorrespondence.entrySet()) {
            String gameObservation = entry.getKey();
            Set<String> variables = new HashSet<>();

            // Iterate over the predicates searching for variables
            for (String observation: entry.getValue()) {
                Matcher variableMatcher = variablePattern.matcher(observation);

                while (variableMatcher.find()) {
                    for (int i = 0; i <= variableMatcher.groupCount(); i++) {
                        variables.add(variableMatcher.group(i));
                    }
                }
            }

            // Add the predicates associated to the game element
            varsFromPredicates.put(gameObservation, variables);
        }

        return varsFromPredicates;
    }

    /**
     * Method that checks whether a single effect predicate is contained
     * in the agenda.
     * @param effect Effect predicate to be checked.
     * @return Returns a PDDLSingleGoal instance which contains the effect
     * predicate if it has been found in the agenda and null otherwise.
     */
    private PDDLSingleGoal checkSingleEffect(String effect) {
        PDDLSingleGoal modifiedGoal = null;
        PDDLSingleGoal pendingGoal = agenda.containedPredicateInPendingGoals(effect);
        PDDLSingleGoal preemptedGoal = agenda.containedPredicateInPreemptedGoals(effect);

        if (pendingGoal != null) {
            agenda.setReachedFromPending(pendingGoal);
            modifiedGoal = pendingGoal;
        } else if (preemptedGoal != null) {
            agenda.setReachedFromPreempted(preemptedGoal);
            modifiedGoal = preemptedGoal;
        }

        return modifiedGoal;
    }

    /**
     * Method that creates a PDDL problem file. It writes the PDDL predicates, variables
     * and the current goal to the problem file.
     */
    private void createProblemFile() {
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

    /**
     * Method that reads the content of a given file.
     * @param filename Path of the file to be read.
     * @return Returns the content of the file.
     */
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

    /**
     * Method that prints an array of messages and asks the user to select some
     * of the available options. These options include displaying information about
     * the agenda, displaying information about the current plan or continuing the
     * program's execution. This method is used when the debug mode is enabled.
     * @param messages Messages to be printed.
     */
    private void displayDebugInformation(String[] messages) {
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

    /**
     * Method that prints an array of messages and waits for the user's input.
     * This method is used when the debug mode is enabled.
     * @param messages Messages to be printed.
     */
    private void showMessagesWait(String[] messages) {
        // Show messages
        this.printMessages(messages);

        Scanner scanner = new Scanner(System.in);

        System.out.println("Press [ENTER] to continue");
        scanner.nextLine();
    }

    /**
     * Method that prints an array of messages. This method is used when the debug
     * mode is enabled.
     * @param messages Messages to be printed.
     */
    private void printMessages(String[] messages) {
        for (String m: messages) {
            System.out.println(m);
        }
    }
}
