package controller;

import java.util.Arrays;
import java.util.List;
import java.util.regex.*;
import java.util.ArrayList;

public class Action {
    private String actionName;
    private List<String> preconditions;

    public Action(String actionName, String actionDescription) {
        this.actionName = actionName;
        this.processPreconditionsFromActionDescription(actionDescription);
    }

    private void processPreconditionsFromActionDescription(String action) {
        // Crete pattern
        Pattern preconditionPattern = Pattern.compile(":precondition[^:]+");

        // Get match of the pattern
        Matcher preconditionMatcher = preconditionPattern.matcher(action);
        // Should this call be controlled in case no match is found?
        preconditionMatcher.find();

        // The preconditions are in the first match since there's only one
        String preconditionsMatch = preconditionMatcher.group(0);

        // Remove all extra spaces, spaces between consequent parentheses and ":precondition" at the beginning
        preconditionsMatch = preconditionsMatch.replaceAll("\\s+", " ");
        preconditionsMatch = preconditionsMatch.substring(0, preconditionsMatch.length() - 1);
        preconditionsMatch = preconditionsMatch.replaceAll(" \\)", ")");
        preconditionsMatch = preconditionsMatch.replaceAll(":precondition\\s+", "");

        // Remove and statement, if there's any. Also removes last parenthesis
        if (preconditionsMatch.contains("and")) {
            preconditionsMatch = preconditionsMatch.replaceFirst("\\(and ", "");
            preconditionsMatch = preconditionsMatch.substring(0, preconditionsMatch.length() - 1);
        }

        // Split preconditions in different lines
        preconditionsMatch = preconditionsMatch.replaceAll("\\) \\(", "\\)\n\\(");

        // Get preconditions as a list
        this.preconditions = new ArrayList<>(Arrays.asList(preconditionsMatch.split("\n")));
    }

    public String getActionName() {
        return this.actionName;
    }

    public List<String> getPreconditions() {
        return this.preconditions;
    }

    @Override
    public String toString() {
        return "Action{" +
                "actionName='" + actionName + '\'' +
                ", preconditions='" + preconditions + '\'' +
                '}';
    }
}
