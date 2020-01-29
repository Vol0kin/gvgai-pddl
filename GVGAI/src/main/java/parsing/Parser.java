package parsing;

// Import GVGAI
import core.game.StateObservation;
import core.game.Observation;
import core.vgdl.VGDLRegistry;

// Import gson to parse JSON files
import tools.com.google.gson.Gson;
import tools.com.google.gson.stream.JsonReader;

// Import java utils (maps, arrays...)
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

import tools.Vector2d;
// Import IO


public class Parser {

    /**
     * Method that returns a new Map that contains a parsed JSON object.
     * This method always returns, whether the JSON file exists or not.
     *
     * @param <K> This describes the Map's key type.
     * @param <V> This describes the Map's value type.
     * @param fileName Name of the file to be parsed.
     * @return Returns a new Map<K, V> that contains the JSON object specified
     *         by fileName.
     */
    public static <K, V> Map<K, V> parseJSONFile(String fileName) {

        // Create a new Gson object, which will read the JSON file
        Gson gson = new Gson();

        // Create a new Map using the HashMap interface
        Map<K, V> map = new HashMap<>();

        try {
            // Read the JSON file
            JsonReader reader = new JsonReader(new FileReader(fileName));

            // Transform the JSON object into a Map
            map = gson.fromJson(reader, map.getClass());
        } catch (FileNotFoundException e) {
            System.err.println(e);
        }

        return map;
    }

    /**
     * Method that transforms a given state of the current game to a 2D matrix
     * of String, which represents the game's map.
     *
     * @param so Observation of the current game state.
     * @return Returns a 2D String matrix containing the current state of the game.
     */
    public static String[][] parseStateObservation(StateObservation so) {

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

    /**
     * Method that obtains the variables that can be found in the predicates
     * associated to each of the game elements.
     *
     * @param correspondence Map that contains the correspondence between the
     *                       game element and the predicates associated to it.
     * @param variables Set of variables that the user has defined.
     * @return Returns a Map that associates game elements and variables.
     */
    public static Map<String, Set<String>> getVariablesFromPredicates(Map<String, ArrayList<String>> correspondence,
                                                                      Set<String> variables)
    {
        /*
         * Create a map that will contain the game element and the variables
         * associated to the predicates for that element
         */
        Map<String, Set<String>> varsFromPredicates = new HashMap<>();

        // Iterate over the correspondence map
        for (Map.Entry<String, ArrayList<String>> entry : correspondence.entrySet()) {
            // Get the key, which represent the game element
            String key = entry.getKey();

            // Create a new set of contained variables in the predicates
            Set<String> containedVars = new HashSet<>();

            /*
             * Iterate over each predicate and each variable to find
             * which ones of the variables are contained in the predicates
             * corresponding to the game element and add them to the set
             */
            for (String predicate : entry.getValue()) {
                for (String var : variables) {
                    if (predicate.contains(var)) {
                        containedVars.add(var);
                    }
                }
            }

            // Update the map entry
            varsFromPredicates.put(key, containedVars);
        }

        return varsFromPredicates;
    }


    /**
     * WIP
     * @param gameMap
     * @param correspondence
     * @param variables
     * @param predicateVars
     * @param connections
     */
    public static void parseGameToPDDL(String[][] gameMap,
                                       Map<String, ArrayList<String>> correspondence,
                                       Map<String, String> variables,
                                       Map<String, Set<String>> predicateVars,
                                       Map<String, String> connections,
                                       String goalPredicate,
                                       String goalVariable,
                                       Vector2d orientation,
                                       Vector2d goalPosition)
    {
        Map<String, Integer> numVariables = new HashMap<>();
        Map<String, ArrayList<String>> objects = new HashMap<>();

        String playerOrientation = "";
        String outGoal = goalPredicate;

        for (String key: variables.keySet()) {
            numVariables.put(key, 0);
            objects.put(key, new ArrayList<>());
        }


        //System.out.println(numVariables);
        //System.out.println(correspondence);
        //System.out.println(predicateVars);

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
                        numVariables.put(var, numVariables.get(var) + 1);
                        String objectNum = var + numVariables.get(var);
                        objects.get(var).add(objectNum);

                        // Create the connections
                        if (var.equals("cell")) {
                            int numCells = objects.get("cell").size();
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
                       // System.out.println(predicateVars);

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

                            // This part will be modified later on
                            /*if (goalPredicates.keySet().contains(var) && goalPosition.x == x && goalPosition.y == y) {
                                if (var.equals("gem")) {
                                    int indexLast = objects.get(var).size() - 1;
                                    outGoal = goalPredicates.get(var).replace(var, objects.get(var).get(indexLast));
                                } else if (var.equals("exit")) {
                                    outGoal = "(exited-level player1)";
                                }

                            }*/


                        }
                        predicateList.add(outPredicate);
                    }

                    // Check whether the current cell is a goal
                    if (x == goalPosition.x && y == goalPosition.y) {
                        if (!goalVariable.equals("")) {
                            outGoal = outGoal.replace(goalVariable, goalVariable + "_" + x + "_" + y);
                        }
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
            // THIS PART HAS TO CHANGE LATER ON
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

        //System.out.println(numVariables);
        //System.out.println(predicateList);
        //System.out.println(objects);
        //System.out.println(connectionSet);
    }
}