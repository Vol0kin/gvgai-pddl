package controller;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import ontology.Types;

public class GameInformation {
    public String domainName;
    public Map<String, ArrayList<String>> gameElementsCorrespondence;
    public Map<Position, String> orientationCorrespondence;
    public LinkedList<PDDLSingleGoal> goals;
    public Map<String, Types.ACTIONS> actionsCorrespondence;
    public Map<Position, String> connections;
    public Map<String, String> variablesTypes;
    public String cellVariable;
    public String avatarVariable;

    public GameInformation() { }
}
