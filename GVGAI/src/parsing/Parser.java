package parsing;

// Import gson to parse JSON files
import tools.com.google.gson.Gson;
import tools.com.google.gson.stream.JsonReader;

// Import java utils (maps, arrays...)
import java.util.HashMap;
import java.util.Map;

// Import IO
import java.io.FileReader;
import java.io.FileNotFoundException;

public class Parser {
    /*
     * Returns a new Map that contains a parsed JSON object.
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
}