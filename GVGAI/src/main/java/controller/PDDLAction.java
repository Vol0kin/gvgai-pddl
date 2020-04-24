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

import java.util.*;
import java.util.regex.*;

import ontology.Types;

/**
 * Class that represents a PDDL action. A PDDLAction object contains a PDDL instantiated
 * action, the corresponding GVGAI action and all the preconditions that must be meet in order
 * to execute the action.
 *
 * @author Vladislav Nikolov Vasilev
 */
public class PDDLAction {
    private String actionInstance;
    private Types.ACTIONS GVGAIAction;
    private List<String> preconditions;

    /**
     * Class constructor.
     *
     * @param actionInstance String that contains the name of the PDDL action and its
     *                       parameters.
     * @param actionDescription String that contains the description of an action
     *                          (parameters, preconditions and effects).
     * @param actionCorrespondence Map that contains the correspondence from a PDDL
     *                             action to a GVGAI action.
     */
    public PDDLAction(String actionInstance, String actionDescription, Map<String, Types.ACTIONS> actionCorrespondence) {
        this.actionInstance = actionInstance;
        this.translateActionInstanceToGVGAI(actionCorrespondence);
        this.processPreconditionsFromActionDescription(actionDescription);
        this.processEffectsFromActionDescription(actionDescription);
    }

    /**
     * Method that reads an action's description and obtains all the preconditions
     * associated to that action. All of the preconditions are instantiated.
     *
     * @param actionDescription String that contains the description of an action
     *                          (parameters, preconditions and effects).
     */
    private void processPreconditionsFromActionDescription(String actionDescription) {
        // Get preconditions match
        String preconditionsMatch = this.matchFormatPattern(Pattern.compile(":precondition[^:]+"),
                actionDescription,
                ":precondition");

        // Split preconditions in different lines
        preconditionsMatch = preconditionsMatch.replaceAll("\\) \\(", "\\)\n\\(");

        // Get preconditions as a list
        this.preconditions = new ArrayList<>(Arrays.asList(preconditionsMatch.split("\n")));
    }

    /**
     * Method that transforms the PDDL action instance into a GVGAI action.
     * It used a Map that is passed as parameter in which each PDDL action
     * is associated to a GVGAI action.
     *
     * @param actionCorrespondence Correspondence between PDDL and GVGAI actions.
     */
    private void translateActionInstanceToGVGAI(Map<String, Types.ACTIONS> actionCorrespondence) {
        // Create pattern
        Pattern actionPattern = Pattern.compile("[^( ]+");

        // Get match of the pattern
        Matcher actionMatcher = actionPattern.matcher(this.actionInstance);
        actionMatcher.find();

        // The action is the first element
        String action = actionMatcher.group(0).toUpperCase();

        // Get the GVGAI Action
        this.GVGAIAction = actionCorrespondence.get(action);
    }

    private void processEffectsFromActionDescription(String actionDescription){
        // Get effects match
        String effectsMatch = this.matchFormatPattern(Pattern.compile(":effect[^:]+"),
                actionDescription,
                ":effect");


        List<String> effectsList = this.splitEffects(effectsMatch);
        System.out.println(effectsList);

        Scanner sc = new Scanner(System.in);
        sc.nextLine();
    }

    private String matchFormatPattern(Pattern pattern, String description, String actionPart) {
        // Get match of the pattern
        Matcher matcher = pattern.matcher(description);
        matcher.find();

        String match = matcher.group(0);

        System.out.println(match);

        // Remove all extra spaces, spaces between consequent parentheses and ":effect" at the beginning
        match = match.replaceAll("\\s+", " ");
        match = match.substring(0, match.length() - 1);
        match = match.replaceAll(" \\)", ")");
        match = match.replaceAll(String.format("%s\\s+", actionPart), "");

        // Remove and statement, if there's any. Also removes last parenthesis
        if (match.contains("and")) {
            match = match.replaceFirst("\\(and ", "");
            match = match.substring(0, match.length() - 1);
        }

        // Remove all extra parentheses
        int numOpenParentheses = match.length() - match.replace("(", "").length();
        int numCloseParentheses = match.length() - match.replace(")", "").length();
        int diffParentheses = numOpenParentheses - numCloseParentheses;

        if (numOpenParentheses - numCloseParentheses != 0) {
            match = match.substring(0, match.length() - Math.abs(diffParentheses));
        }

        return match;
    }

    private List<String> splitEffects(String effects) {
        List<String> effectsList = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        int numParentheses = 0;

        for (char c: effects.toCharArray()) {
            boolean changed = false;

            if (c == '(') {
                numParentheses++;
                changed = true;
            } else if (c == ')') {
                numParentheses--;
                changed = true;
            }

            builder.append(c);

            // Convert builder to string if a final closing parenthesis is found
            if (numParentheses == 0 && changed) {
                effectsList.add(builder.toString().trim());
                builder.setLength(0);
            }
        }

        return effectsList;
    }

    /**
     * PDDL action instance getter.
     * @return Returns the PDDL action instance.
     */
    public String getActionInstance() {
        return this.actionInstance;
    }

    /**
     * Preconditions getter.
     * @return Returns the instantiated preconditions of a given PDDL actions.
     */
    public List<String> getPreconditions() {
        return this.preconditions;
    }

    /**
     * GVGAI action getter.
     * @return Returns the GVGAI action associated to the given PDDL action.
     */
    public Types.ACTIONS getGVGAIAction() {
        return this.GVGAIAction;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("\n\n\t### Action ###");
        builder.append(String.format("\n\t|--- Instance: %s", this.actionInstance));
        builder.append(String.format("\n\t|--- GVGAI action: %s", this.GVGAIAction));
        builder.append(String.format("\n\t|--- List of preconditions: %s", this.preconditions));

        return builder.toString();
    }
}
