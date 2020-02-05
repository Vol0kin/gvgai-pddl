package controller;

public class GoalState {
    private String goalDescription;
    private int priority;

    public GoalState(String goalDescription, int priority) {
        this.goalDescription = goalDescription;
        this.priority = priority;
    }

    public String getGoalDescription() {
        return this.goalDescription;
    }

    public int getPriority() {
        return this.priority;
    }

    @Override
    public String toString() {
        return "GoalState{" +
                "goalDescription='" + goalDescription + '\'' +
                ", priority=" + priority +
                '}';
    }
}
