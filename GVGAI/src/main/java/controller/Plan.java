package controller;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import ontology.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.stream.Collectors;


public class Plan implements Iterable<Action>{
    private List<Action> actions;

    public Plan() {
        this.actions = new ArrayList<>();
    }

    public Plan(JSONObject response, Map<String, Types.ACTIONS> actionCorrespondence) {
        System.out.println(response);
        JSONArray plan = response.getJSONObject("result").getJSONArray("plan");

        ArrayList<Action> actionList = new ArrayList<>();

        this.actions = new ArrayList<>();

        for (int i = 0; i < plan.length(); i++) {
            JSONObject planElement = plan.getJSONObject(i);

            String actionName = planElement.getString("name");
            String preconditions = planElement.getString("action");

            actionList.add(new Action(actionName, preconditions, actionCorrespondence));
        }

        // Remove null actions
        this.actions = actionList
                        .stream()
                        .filter(action -> action.getGVGAIAction() != null)
                        .collect(Collectors.toList());

        System.out.println(this.actions);
    }

    public List<Action> getActions() {
        return this.actions;
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
    public Iterator<Action> iterator() {
        Iterator<Action> iterator = new Iterator<Action>() {
            private int currentIdx = 0;

            @Override
            public boolean hasNext() {
                return currentIdx < actions.size() && actions.get(currentIdx) != null;
            }

            @Override
            public Action next() {
                return actions.get(currentIdx++);
            }
        };

        return iterator;
    }
}
