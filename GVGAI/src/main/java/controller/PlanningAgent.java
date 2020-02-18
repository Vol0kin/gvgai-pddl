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

import core.player.AbstractPlayer;
import core.game.StateObservation;
import parsing.Parser;
import tools.ElapsedCpuTimer;

import ontology.Types;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import tools.Vector2d;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

public class PlanningAgent extends AbstractPlayer {

    protected Map<String, ArrayList<String>> correspondence;
    protected Map<String, String> variables;
    protected Map<String, Set<String>> predicateVars;
    protected Map<String, String> connections;
    protected Map<String, Types.ACTIONS> actionCorrespondence;

    protected Map<String, String> goalVariablesMap;

    protected List<Types.ACTIONS> actionList;
    protected LinkedList<PDDLSingleGoal> goalsList;
    protected Agenda agenda;

    protected List<String> PDDLGameStatePredicates;
    protected Map<String, LinkedHashSet<String>> PDDLGameStateObjects;

    protected PDDLPlan PDDLPlan;
    protected Iterator<PDDLAction> iterPlan;

    protected boolean mustReplan;

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

        // Initialize PDDL game state information
        this.PDDLGameStatePredicates = new ArrayList<>();
        this.PDDLGameStateObjects = new HashMap<>();
        this.variables
                .keySet()
                .stream()
                .forEach(key -> this.PDDLGameStateObjects.put(key, new LinkedHashSet<>()));

        this.PDDLPlan = new PDDLPlan();
        this.iterPlan = PDDLPlan.iterator();


        this.goalsList = new LinkedList<>();
        // Las gemas en las que se tienen que picar 2 o más rocas seguidas dan problemas
        this.goalsList.addLast(new PDDLSingleGoal("(got gem_16_9)", 1));
        this.goalsList.addLast(new PDDLSingleGoal("(got gem_7_3)", 1));
        this.goalsList.addLast(new PDDLSingleGoal("(got gem_6_3)", 1));
        this.goalsList.addLast(new PDDLSingleGoal("(got gem_5_3)", 1));
        this.goalsList.addLast(new PDDLSingleGoal("(got gem_1_4)", 1));
        this.goalsList.addLast(new PDDLSingleGoal("(got gem_6_1)", 1));
        this.goalsList.addLast(new PDDLSingleGoal("(got gem_7_1)", 1));
        this.goalsList.addLast(new PDDLSingleGoal("(got gem_7_9)", 1));
        this.goalsList.addLast(new PDDLSingleGoal("(got gem_9_10)", 1));
        this.goalsList.addLast(new PDDLSingleGoal("(exited-level)", 2));

        this.agenda = new Agenda(this.goalsList);

        this.mustReplan = true;
    }

    @Override
    public Types.ACTIONS act(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        Types.ACTIONS action;

        // Get the player's orientation
        Vector2d orientation = stateObservation.getAvatarOrientation();

        this.parseGameStateToPDDL(stateObservation, correspondence, predicateVars, connections, orientation);

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
        String domain = readFile("planning/domain.pddl"),
               problem = readFile("planning/problem.pddl");

        // Call online planner and get its response
        HttpResponse<JsonNode> response = Unirest.post("http://solver.planning.domains/solve")
                .header("accept", "application/json")
                .field("domain", domain)
                .field("problem", problem)
                .asJson();

        // Here the response should be checked in case there's been an error

        // Create a new PDDLPlan instance if a valid plan has been found
        PDDLPlan PDDLPlan = new PDDLPlan(response.getBody().getObject(), this.actionCorrespondence);

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

    public void parseGameStateToPDDL(StateObservation stateObservation,
                                     Map<String, ArrayList<String>> correspondence,
                                     Map<String, Set<String>> predicateVars,
                                     Map<String, String> connections,
                                     Vector2d orientation)
    {
        String[][] gameMap = Parser.parseStateObservation(stateObservation);
        this.PDDLGameStatePredicates.clear();
        this.PDDLGameStateObjects.values().stream().forEach(x -> x.clear());

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
                                String replacement = var.equals("player") ? var : var + "_" + x + "_" + y;
                                outPredicate = outPredicate.replace(var, replacement);

                                if (var.equals("player")) {
                                    if (orientation.x == 1.0) {
                                        this.PDDLGameStatePredicates.add("(oriented-right player)");
                                    } else if (orientation.x == -1.0) {
                                        this.PDDLGameStatePredicates.add("(oriented-left player)");
                                    } else if (orientation.y == 1.0) {
                                        this.PDDLGameStatePredicates.add("(oriented-down player)");
                                    } else if (orientation.y == -1.0) {
                                        this.PDDLGameStatePredicates.add("(oriented-up player)");
                                    }
                                }

                                this.PDDLGameStateObjects.get(var).add(replacement);
                            }
                        }
                        this.PDDLGameStatePredicates.add(outPredicate);
                    }
                }
            }
        }

        // Add connections to predicates
        connectionSet.stream().forEach(connection -> this.PDDLGameStatePredicates.add(connection));
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
            bf.write("(:domain Boulderdash)");
            bf.newLine();

            // Write the objects
            // Each variable will be written
            bf.write("(:objects");
            bf.newLine();

            // Write each object
            for (String key: this.PDDLGameStateObjects.keySet()) {
                if (!this.PDDLGameStateObjects.get(key).isEmpty()) {
                    String objectsStr = String.join(" ", this.PDDLGameStateObjects.get(key));
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
