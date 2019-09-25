package parsing;

// Import GVGAI
import core.game.StateObservation;
import core.game.Observation;
import core.vgdl.VGDLRegistry;

// Import gson to parse JSON files
import tools.com.google.gson.Gson;
import tools.com.google.gson.stream.JsonReader;

// Import java utils (maps, arrays...)
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

// Import IO
import java.io.FileReader;
import java.io.FileNotFoundException;

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
                                       Map<String, String> connections)
    {
        Map<String, Integer> numVariables = new HashMap<>();

        for (String key: variables.keySet()) {
            numVariables.put(key, 0);
        }

        System.out.println(numVariables);
        System.out.println(correspondence);
        System.out.println(predicateVars);

        ArrayList<String> predicateList = new ArrayList<>();
        ArrayList<String> connectionList = new ArrayList<>();

        final int X_MAX = gameMap.length, Y_MAX = gameMap[0].length;

        for (int y = 0; y < Y_MAX; y++) {
            for (int x = 0; x < X_MAX; x++) {
                String cellType = gameMap[x][y];

                if (predicateVars.containsKey(cellType)) {
                    // Increase the number of variables from that type
                    for (String var: predicateVars.get(cellType)) {
                        numVariables.put(var, numVariables.get(var) + 1);
                    }


                }
            }
        }

        System.out.println(numVariables);
    }
}