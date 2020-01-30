package controller;

import core.game.Observation;
import core.player.AbstractPlayer;
import core.game.StateObservation;
import parsing.Parser;
import tools.ElapsedCpuTimer;

import ontology.Types;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import parsing.Parser;
import tools.Vector2d;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

public class PlanningAgent extends AbstractPlayer {

    protected Map<String, ArrayList<String>> correspondence;
    protected Map<String, String> variables;
    protected Map<String, Set<String>> predicateVars;
    protected Map<String, String> connections;
    protected Map<String, Types.ACTIONS> actionCorrespondence;

    protected Map<String, String> goalVariablesMap;

    protected List<Types.ACTIONS> actionList;
    protected int blockSize;
    protected int numGems;
    protected static int MAX_GEMS = 9;
    protected LinkedList<String> agenda;

    public PlanningAgent(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        this.actionCorrespondence = new HashMap<>();
        this.correspondence = Parser.<String, ArrayList<String>>parseJSONFile("JSON/correspondence.json");
        this.variables = Parser.<String, String>parseJSONFile("JSON/variables.json");
        this.predicateVars = Parser.getVariablesFromPredicates(correspondence, variables.keySet());
        this.connections = Parser.parseJSONFile("JSON/connections.json");
        Map<String, String> actions = Parser.parseJSONFile("JSON/actions.json");

        for (Map.Entry<String, String> entry: actions.entrySet()) {
            actionCorrespondence.put(entry.getKey(), Types.ACTIONS.fromString(entry.getValue()));
        }

        this.goalVariablesMap = new HashMap<>();

        this.goalVariablesMap.put("(got gem)", "gem");
        this.goalVariablesMap.put("(exited-level)", "");

        System.out.println(actionCorrespondence);
        System.out.println(variables);
        System.out.println(predicateVars);

        this.actionList = new ArrayList<>();
        this.blockSize = stateObservation.getBlockSize();
        this.numGems = 0;

        this.agenda = new LinkedList<>();
        // Las gemas en las que se tienen que picar 2 o más rocas seguidas dan problemas
        this.agenda.addLast("(got gem_16_9)");
        this.agenda.addLast("(got gem_7_3)");
        this.agenda.addLast("(got gem_6_3)");
        this.agenda.addLast("(got gem_5_3)");
        this.agenda.addLast("(got gem_1_4)");
        this.agenda.addLast("(got gem_6_1)");
        this.agenda.addLast("(got gem_7_1)");
        this.agenda.addLast("(got gem_7_9)");
        this.agenda.addLast("(got gem_9_10)");
        this.agenda.addLast("(exited-level)");
    }

    @Override
    public Types.ACTIONS act(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        Types.ACTIONS action;

        if (actionList.isEmpty()) {
            System.out.println("I need to find a plan!");

            // Get the player's orientation
            Vector2d orientation = stateObservation.getAvatarOrientation();

            //System.out.println(Parser.<String, ArrayList<String>>parseJSONFile("correspondence.json").get("A").get(0));
            //System.out.println(VGDLRegistry.GetInstance().getRegisteredSpriteKey(10));
            String[][] gameMap = Parser.parseStateObservation(stateObservation);

            long time = elapsedCpuTimer.remainingTimeMillis();
            this.parseGameToPDDL(gameMap, correspondence, variables, predicateVars, connections, orientation);
            System.out.println("Consumed time: " + (time - elapsedCpuTimer.remainingTimeMillis()));

            //Determine an index randomly and get the action to return.
            action = Types.ACTIONS.ACTION_NIL;

            time = elapsedCpuTimer.remainingTimeMillis();
            callPlanner();
            System.out.println("Consumed time waiting for planner's response: " + (time - elapsedCpuTimer.remainingTimeMillis()));
            this.actionList = translateOutputPlan();
        } else {
            System.out.println(this.actionList);
            action = this.actionList.get(0);
            this.actionList.remove(0);

            if (this.actionList.isEmpty()) {
                this.agenda.removeFirst();
            }
        }

        //Return the action.
        return action;
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

    public void callOnlinePlanner() {
        String domain = readFile("planning/domain.pddl"),
               problem = readFile("planning/problem.pddl");

        HttpResponse<JsonNode> response = Unirest.post("http://solver.planning.domains/solve")
                .header("accept", "application/json")
                .field("domain", domain)
                .field("problem", problem)
                .asJson();

        System.out.println(response.getBody());
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

    public void parseGameToPDDL(String[][] gameMap,
                                Map<String, ArrayList<String>> correspondence,
                                Map<String, String> variables,
                                Map<String, Set<String>> predicateVars,
                                Map<String, String> connections,
                                Vector2d orientation)
    {
        Map<String, LinkedHashSet<String>> objects = new HashMap<>();

        String playerOrientation = "";
        String outGoal = this.agenda.getFirst();

        for (String key: variables.keySet()) {
            objects.put(key, new LinkedHashSet<>());
        }

        ArrayList<String> predicateList = new ArrayList<>();
        Set<String> connectionSet = new LinkedHashSet<>();

        final int X_MAX = gameMap.length, Y_MAX = gameMap[0].length;

        for (int y = 0; y < Y_MAX; y++) {
            for (int x = 0; x < X_MAX; x++) {
                // Get the cells representation as a game element
                String cellType = gameMap[x][y];
                String currentCell = "cell_" + x + "_" + y;

                // Check if there are predicates associated to the game element
                if (predicateVars.containsKey(cellType)) {

                    // Increase the number of variables from that type
                    for (String var: predicateVars.get(cellType)) {
                        // Create the connections
                        if (var.equals("cell")) {
                            if (y - 1 >= 0) {
                                String connection = connections.get("up");
                                connection = connection.replace("?c", currentCell);
                                connection = connection.replace("?p", "cell_" + x + "_" + (y-1));
                                connectionSet.add(connection);
                            }

                            if (y + 1 < Y_MAX) {
                                String connection = connections.get("down");
                                connection = connection.replace("?c", currentCell);
                                connection = connection.replace("?n", "cell_" + x + "_" + (y+1));
                                connectionSet.add(connection);
                            }

                            if (x - 1 >= 0) {
                                String connection = connections.get("left");
                                connection = connection.replace("?c", currentCell);
                                connection = connection.replace("?p", "cell_" + (x-1) + "_" + y);
                                connectionSet.add(connection);
                            }

                            if (x + 1 < X_MAX) {
                                String connection = connections.get("right");
                                connection = connection.replace("?c", currentCell);
                                connection = connection.replace("?n", "cell_" + (x+1) + "_" + y);
                                connectionSet.add(connection);
                            }
                        }
                    }

                    // Add to each variable in each predicate its number
                    for (String pred: correspondence.get(cellType)) {
                        // Create new empty output predicate
                        String outPredicate = pred;

                        for (String var: predicateVars.get(cellType)) {

                            if (pred.contains(var)) {
                                String replacement = var + "_" + x + "_" + y;
                                outPredicate = outPredicate.replace(var, replacement);

                                if (var.equals("player")) {
                                    if (orientation.x == 1.0) {
                                        playerOrientation = "(oriented-right player" + "_" + x + "_" + y + ")";
                                    } else if (orientation.x == -1.0) {
                                        playerOrientation = "(oriented-left player" + "_" + x + "_" + y + ")";
                                    } else if (orientation.y == 1.0) {
                                        playerOrientation = "(oriented-down player" + "_" + x + "_" + y + ")";
                                    } else if (orientation.y == -1.0) {
                                        playerOrientation = "(oriented-up player" + "_" + x + "_" + y + ")";
                                    }
                                }

                                objects.get(var).add(replacement);
                            }
                        }
                        predicateList.add(outPredicate);
                    }
                }
            }
        }

        try (BufferedWriter bf = new BufferedWriter(new FileWriter("planning/problem.pddl"))) {
            // Write problem name
            // THIS LINE HAS TO BE CHANGED LATER ON, ALLOWING TO CHANGE THE PROBLEM
            bf.write("(define (problem Boulders)");
            bf.newLine();

            // Write domain that is used
            // THIS LINE HAS TO BE CHANGED LATER ON, ALLOWING AUTOMATIC CHANGE
            bf.write("(:domain Boulderdash)");
            bf.newLine();

            // Write the objects
            // Each variable will be written
            bf.write("(:objects");
            bf.newLine();

            // Write each object
            for (String key: objects.keySet()) {
                if (!objects.get(key).isEmpty()) {
                    String objectsStr = String.join(" ", objects.get(key));
                    objectsStr += String.format(" - %s", variables.get(key));
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
            for (String line: predicateList) {
                bf.write(line);
                bf.newLine();
            }

            // Write orientation
            bf.write(playerOrientation);
            bf.newLine();

            // Write each connection into the file
            for (String connection: connectionSet) {
                bf.write(connection);
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
