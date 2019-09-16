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

        // Iterate over the map and transform it to string
        // The VGDLRegistry contains information that will allow us to transform
        // the current StateObservation to a matrix
        for (int y = 0; y < Y_MAX; y++) {
            for (int x = 0; x < X_MAX; x++) {
                // If there's an observation, get its name using the itype
                // information
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
}