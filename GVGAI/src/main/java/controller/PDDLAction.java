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
import tools.Pair;

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
    private List<Effect> effects;

    public class Effect {
        private String effectPredicate;
        private List<String> conditions;

        public Effect(String effectPredicate, List<String> conditions) {
            this.effectPredicate = effectPredicate;
            this.conditions = conditions;
        }

        public Effect(String effectPredicate) {
            this.effectPredicate = effectPredicate;
            this.conditions = new ArrayList<>();
        }

        public String getEffectPredicate() {
            return this.effectPredicate;
        }

        public List<String> getConditions() {
            return this.conditions;
        }

        @Override
        public String toString() {
            return "{ Effect predicate: " + this.effectPredicate + ", Conditions: " + this.conditions + " }";
        }

    }

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
        this.GVGAIAction = this.translateActionInstanceToGVGAI(actionCorrespondence);
        this.preconditions = this.processPreconditionsFromActionDescription(actionDescription);
        this.effects = this.processEffectsFromActionDescription(actionDescription);
    }

    /**
     * Method that reads an action's description and obtains all the preconditions
     * associated to that action. All of the preconditions are instantiated.
     *
     * @param actionDescription String that contains the description of an action
     *                          (parameters, preconditions and effects).
     * @return
     */
    private ArrayList<String> processPreconditionsFromActionDescription(String actionDescription) {
        // Get preconditions match
        String preconditionsMatch = this.matchFormatPattern(Pattern.compile(":precondition[^:]+"),
                actionDescription,
                ":precondition");

        // Split preconditions in different lines
        preconditionsMatch = preconditionsMatch.replaceAll("\\) \\(", "\\)\n\\(");

        // Get preconditions as a list
        return new ArrayList<>(Arrays.asList(preconditionsMatch.split("\n")));
    }

    /**
     * Method that transforms the PDDL action instance into a GVGAI action.
     * It used a Map that is passed as parameter in which each PDDL action
     * is associated to a GVGAI action.
     *
     * @param actionCorrespondence Correspondence between PDDL and GVGAI actions.
     * @return
     */
    private Types.ACTIONS translateActionInstanceToGVGAI(Map<String, Types.ACTIONS> actionCorrespondence) {
        // Create pattern
        Pattern actionPattern = Pattern.compile("[^( ]+");

        // Get match of the pattern
        Matcher actionMatcher = actionPattern.matcher(this.actionInstance);
        actionMatcher.find();

        // The action is the first element
        String action = actionMatcher.group(0).toUpperCase();

        // Get the GVGAI Action
        return actionCorrespondence.get(action);
    }

    private List<Effect> processEffectsFromActionDescription(String actionDescription){
        // Get effects match
        String effectsMatch = this.matchFormatPattern(Pattern.compile(":effect[^:]+"),
                actionDescription,
                ":effect");


        List<String> splitEffectsList = this.splitDescriptionIntoBlocks(effectsMatch);
        List<Effect> actionEffects = new ArrayList<>();

        for (String effect: splitEffectsList) {
            if (effect.contains("when")) {
                Pair<List<String>, List<String>> conditionsEffectsPair = this.splitConditionsFromEffects(effect);
                List<String> conditionsList = conditionsEffectsPair.first;
                List<String> effectsList = conditionsEffectsPair.second;

                effectsList.stream().forEach(e -> actionEffects.add(new Effect(e, conditionsList)));
            } else {
                actionEffects.add(new Effect(effect));
            }
        }

        return actionEffects;
    }

    private String matchFormatPattern(Pattern pattern, String description, String actionPart) {
        // Get match of the pattern
        Matcher matcher = pattern.matcher(description);
        matcher.find();

        String match = matcher.group(0);

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

    private List<String> splitDescriptionIntoBlocks(String description) {
        List<String> descriptionList = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        int numParentheses = 0;

        for (char c: description.toCharArray()) {
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
                descriptionList.add(builder.toString().trim());
                builder.setLength(0);
            }
        }

        return descriptionList;
    }

    private Pair<List<String>, List<String>> splitConditionsFromEffects(String description) {

        // Process description removing (when and last parenthesis
        String processedDescription = description.replace("(when ", "");
        processedDescription = processedDescription.substring(0, processedDescription.length() - 1);

        // Separate effects and conditions in two blocks
        List<String> effectsConditionsBlocks = this.splitDescriptionIntoBlocks(processedDescription);
        String conditions = effectsConditionsBlocks.get(0);
        String effects = effectsConditionsBlocks.get(1);

        // Remove "(and" and last parenthesis, if there's any
        if (conditions.contains("(and")) {
            conditions = conditions.replace("(and", "");
            conditions = conditions.substring(0, conditions.length() - 1);
        }

        if (effects.contains("(and")) {
            effects = effects.replace("(and", "");
            effects = effects.substring(0, effects.length() - 1);
        }

        List<String> conditionsList = this.splitDescriptionIntoBlocks(conditions);
        List<String> effectsList = this.splitDescriptionIntoBlocks(effects);

        Pair<List<String>, List<String>> conditionsEffectsPair = new Pair(conditionsList, effectsList);

        return conditionsEffectsPair;
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

    public List<Effect> getEffects() {
        return this.effects;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("\n\n\t### Action ###");
        builder.append(String.format("\n\t|--- Instance: %s", this.actionInstance));
        builder.append(String.format("\n\t|--- GVGAI action: %s", this.GVGAIAction));
        builder.append(String.format("\n\t|--- List of preconditions: %s", this.preconditions));
        builder.append(String.format("\n\t|--- List of effects: %s", this.effects));

        return builder.toString();
    }
}
