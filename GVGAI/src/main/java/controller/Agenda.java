package controller;

import java.util.List;
import java.util.LinkedList;

public class Agenda {
    private LinkedList<String> notPlannedGoals;
    private LinkedList<String> haltedGoals;
    private LinkedList<String> reachedGoals;
    private String currentGoal;

    public Agenda(LinkedList<String> goals) {
        this.notPlannedGoals = goals;
        this.haltedGoals = new LinkedList<>();
        this.reachedGoals = new LinkedList<>();
        this.currentGoal = "";
    }

    public LinkedList<String> getNotPlannedGoals() {
        return this.notPlannedGoals;
    }

    public LinkedList<String> getHaltedGoals() {
        return this.haltedGoals;
    }

    public LinkedList<String> getReachedGoals() {
        return this.reachedGoals;
    }

    public String getCurrentGoal() {
        return this.currentGoal;
    }

    public boolean setCurrentGoal() {
        boolean setGoal = true;

        if (!this.haltedGoals.isEmpty()) {
            this.currentGoal = this.haltedGoals.removeFirst();
        } else if (!this.notPlannedGoals.isEmpty()) {
            this.currentGoal = this.notPlannedGoals.removeFirst();
        } else {
            setGoal = false;
        }

        return setGoal;
    }

    public void haltCurrentGoal() {
        this.haltedGoals.addLast(this.currentGoal);
        this.currentGoal = "";
    }

    public void updateReachedGoals() {
        this.reachedGoals.addLast(this.currentGoal);
        this.currentGoal = "";
    }

    @Override
    public String toString() {
        return "Agenda{" +
                "notPlannedGoals=" + notPlannedGoals +
                ", haltedGoals=" + haltedGoals +
                ", reachedGoals=" + reachedGoals +
                ", currentGoal='" + currentGoal + '\'' +
                '}';
    }
}
