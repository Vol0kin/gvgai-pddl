package controller;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import tools.com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;


public class Plan {
    private List<Action> actions;

    public Plan(JSONObject response) {
        JSONArray plan = response.getJSONObject("result").getJSONArray("plan");

        this.actions = new ArrayList<>();

        for (int i = 0; i < plan.length(); i++) {
            JSONObject planElement = plan.getJSONObject(i);

            String actionName = planElement.getString("name");
            String preconditions = planElement.getString("action");

            actions.add(new Action(actionName, preconditions));
        }

        System.out.println(this.actions);
    }

    public List<Action> getActions() {
        return this.actions;
    }
}
