package controller;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import ontology.Types;

public class GameInformation {
    public String domainName;
    public Map<String, ArrayList<String>> gameElementsCorrespondence;
    public Map<Orientation, String> orientationCorrespondence;
    public PDDLSingleGoal goal;
    public ArrayList<PDDLSingleGoal> goals;
    public Map<String, Types.ACTIONS> actionsCorrespondence;
    public Map<String, String> connections;
    public Map<String, String> variablesTypes;

    public GameInformation() { }
}
