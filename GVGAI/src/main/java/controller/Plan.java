package controller;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import ontology.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;


public class Plan implements Iterable<Action>{
    private List<Action> actions;

    public Plan() {
        this.actions = new ArrayList<>();
    }

    public Plan(JSONObject response, Map<String, Types.ACTIONS> actionCorrespondence) {
        JSONArray plan = response.getJSONObject("result").getJSONArray("plan");

        this.actions = new ArrayList<>();

        for (int i = 0; i < plan.length(); i++) {
            JSONObject planElement = plan.getJSONObject(i);

            String actionName = planElement.getString("name");
            String preconditions = planElement.getString("action");

            actions.add(new Action(actionName, preconditions, actionCorrespondence));
        }

        System.out.println(this.actions);
    }

    public List<Action> getActions() {
        return this.actions;
    }

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
