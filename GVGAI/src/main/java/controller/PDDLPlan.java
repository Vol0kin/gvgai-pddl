/*
 * PDDLPlan.java
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

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import ontology.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.stream.Collectors;


public class PDDLPlan implements Iterable<PDDLAction>{
    private List<PDDLAction> PDDLActions;

    public PDDLPlan() {
        this.PDDLActions = new ArrayList<>();
    }

    public PDDLPlan(JSONObject response, Map<String, Types.ACTIONS> actionCorrespondence) {
        System.out.println(response);
        JSONArray plan = response.getJSONObject("result").getJSONArray("plan");

        ArrayList<PDDLAction> PDDLActionList = new ArrayList<>();

        this.PDDLActions = new ArrayList<>();

        for (int i = 0; i < plan.length(); i++) {
            JSONObject planElement = plan.getJSONObject(i);

            String actionName = planElement.getString("name");
            String preconditions = planElement.getString("action");

            PDDLActionList.add(new PDDLAction(actionName, preconditions, actionCorrespondence));
        }

        // Remove null actions
        this.PDDLActions = PDDLActionList
                        .stream()
                        .filter(PDDLAction -> PDDLAction.getGVGAIAction() != null)
                        .collect(Collectors.toList());

        System.out.println(this.PDDLActions);
    }

    public List<PDDLAction> getPDDLActions() {
        return this.PDDLActions;
    }
/*
    public Action getNextAction() {
        Action nextAction = this.actions.get(0);
        this.actions.remove(0);

        return nextAction;
    }

    public boolean isPlanEmpty() {
        return this.actions.isEmpty();
    }*/

    @Override
    public Iterator<PDDLAction> iterator() {
        Iterator<PDDLAction> iterator = new Iterator<PDDLAction>() {
            private int currentIdx = 0;

            @Override
            public boolean hasNext() {
                return currentIdx < PDDLActions.size() && PDDLActions.get(currentIdx) != null;
            }

            @Override
            public PDDLAction next() {
                return PDDLActions.get(currentIdx++);
            }
        };

        return iterator;
    }
}
