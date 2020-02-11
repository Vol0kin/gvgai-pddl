/*
 * PDDLAction.java
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.*;
import java.util.ArrayList;
import ontology.Types;

public class PDDLAction {
    private String actionName;
    private Types.ACTIONS GVGAIAction;
    private List<String> preconditions;

    public PDDLAction(String actionName, String actionDescription, Map<String, Types.ACTIONS> actionCorrespondence) {
        this.actionName = actionName;
        this.translateActionNameToGVGAI(actionCorrespondence);
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

    private void translateActionNameToGVGAI(Map<String, Types.ACTIONS> actionCorrespondence) {
        // Create pattern
        Pattern actionPattern = Pattern.compile("[^( ]+");

        // Get match of the pattern
        Matcher actionMatcher = actionPattern.matcher(this.actionName);
        // Should this call be controlled in case no match is found?
        actionMatcher.find();

        // The action is the first element
        String action = actionMatcher.group(0).toUpperCase();

        // Get the GVGAI Action
        this.GVGAIAction = actionCorrespondence.get(action);
    }

    public String getActionName() {
        return this.actionName;
    }

    public List<String> getPreconditions() {
        return this.preconditions;
    }

    public Types.ACTIONS getGVGAIAction() {
        return this.GVGAIAction;
    }

    @Override
    public String toString() {
        return "Action{" +
                "actionName='" + actionName + '\'' +
                ", GVGAIAction= '" + this.GVGAIAction + '\'' +
                ", preconditions='" + preconditions + '\'' +
                '}';
    }
}
