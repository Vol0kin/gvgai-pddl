package controller;

public class Action {
    private String actionName;
    private String preconditions;

    public Action(String actionName, String preconditions) {
        this.actionName = actionName;
        this.preconditions = preconditions;
    }

    public String getActionName() {
        return this.actionName;
    }

    public String getPreconditions() {
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
