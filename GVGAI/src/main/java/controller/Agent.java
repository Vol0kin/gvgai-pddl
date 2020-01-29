package controller; //The package name is the same as the username in the web.

// Imports related to GVGAI
import core.game.Observation;
import core.player.AbstractPlayer;
import core.game.StateObservation;
import core.game.SerializableStateObservation;
import core.game.GameDescription;
import core.vgdl.VGDLRegistry;
import tools.ElapsedCpuTimer;
import ontology.Types;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.*;

import parsing.Parser;
import tools.Vector2d;

import java.util.Random;

public class Agent extends AbstractPlayer {

    protected Random randomGenerator;
    protected Map<String, ArrayList<String>> correspondence;
    protected Map<String, String> variables;
    protected Map<String, Set<String>> predicateVars;
    protected Map<String, String> connections;
    protected Map<String, Types.ACTIONS> actionCorrespondence;

    protected Map<String, String> goalVariablesMap;

    protected List<Types.ACTIONS> actionList;
    protected int blockSize;
    protected int numGems;
    protected int prevNumGems;
    protected static int MAX_GEMS = 9;

    //Constructor. It must return in 1 second maximum.
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        this.actionCorrespondence = new HashMap<>();
        this.randomGenerator = new Random();
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
        this.blockSize = so.getBlockSize();
        this.numGems = 0;
    }

    //Act function. Called every game step, it must return an action in 40 ms maximum.
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        Types.ACTIONS action;

        if (actionList.isEmpty()) {
            System.out.println("I need to find a plan!");

            // Get the list of current resources
            ArrayList<Observation>[] resources = stateObs.getResourcesPositions();

            // Get exit
            ArrayList<Observation>[] exit = stateObs.getPortalsPositions();

            // Get the player's orientation
            Vector2d orientation = stateObs.getAvatarOrientation();

            Vector2d goalPos;
            Observation goalObservation;

            String goalPredicate, goalVariable;

            if (this.numGems < this.MAX_GEMS) {
                // Select a random goal
                int index = randomGenerator.nextInt(resources[0].size());
                goalObservation = resources[0].get(index);

                // Get the gem goal
                goalPos = goalObservation.position;

                goalPredicate = "(got gem)";

            } else {
                // Get the exit observation
                goalObservation = exit[0].get(0);

                // Get the goal and transform its position to game cells
                goalPos = goalObservation.position;

                goalPredicate = "(exited-level)";
            }

            // Get the goal variable
            goalVariable = this.goalVariablesMap.get(goalPredicate);

            // Get the position of the current goal
            goalPos.x /= blockSize;
            goalPos.y /= blockSize;

            //System.out.println(Parser.<String, ArrayList<String>>parseJSONFile("correspondence.json").get("A").get(0));
            //System.out.println(VGDLRegistry.GetInstance().getRegisteredSpriteKey(10));
            String[][] gameMap = Parser.parseStateObservation(stateObs);

            long time = elapsedTimer.remainingTimeMillis();
            Parser.parseGameToPDDL(gameMap, correspondence, variables, predicateVars, connections, goalPredicate, goalVariable, orientation, goalPos);
            System.out.println("Consumed time: " + (time - elapsedTimer.remainingTimeMillis()));

            //Determine an index randomly and get the action to return.
            action = Types.ACTIONS.ACTION_NIL;

            time = elapsedTimer.remainingTimeMillis();
            callPlanner();
            System.out.println("Consumed time waiting for planner's response: " + (time - elapsedTimer.remainingTimeMillis()));
            this.actionList = translateOutputPlan();
        } else {
            System.out.println(this.actionList);
            action = this.actionList.get(0);
            this.actionList.remove(0);

            if (this.actionList.isEmpty()) {
                this.numGems++;
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
}