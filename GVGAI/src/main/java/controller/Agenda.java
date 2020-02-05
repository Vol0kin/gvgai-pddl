package controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class Agenda {
    private LinkedList<GoalState> notPlannedGoals;
    private LinkedList<GoalState> haltedGoals;
    private LinkedList<GoalState> reachedGoals;
    private GoalState currentGoal;

    public Agenda(LinkedList<GoalState> goals) {
        this.notPlannedGoals = this.sortListByPriority(goals);
        this.haltedGoals = new LinkedList<>();
        this.reachedGoals = new LinkedList<>();
        this.currentGoal = null;
    }

    public LinkedList<GoalState> getNotPlannedGoals() {
        return this.notPlannedGoals;
    }

    public LinkedList<GoalState> getHaltedGoals() {
        return this.haltedGoals;
    }

    public LinkedList<GoalState> getReachedGoals() {
        return this.reachedGoals;
    }

    public GoalState getCurrentGoal() {
        return this.currentGoal;
    }

    public boolean setCurrentGoal() {
        boolean setGoal = true;

        if (!this.notPlannedGoals.isEmpty() && this.haltedGoals.isEmpty()) {
            this.currentGoal = this.notPlannedGoals.removeFirst();
        } else if (this.notPlannedGoals.isEmpty() && !this.haltedGoals.isEmpty()) {
            this.currentGoal = this.haltedGoals.removeFirst();
        } else if (!this.notPlannedGoals.isEmpty() && !this.haltedGoals.isEmpty()) {
            GoalState firstNotPlanned = this.notPlannedGoals.getFirst(),
                      firstHalted = this.haltedGoals.getFirst();

            if (firstHalted.getPriority() < firstNotPlanned.getPriority()) {
                this.currentGoal = firstHalted;
                this.haltedGoals.removeFirst();
            } else {
                this.currentGoal = firstNotPlanned;
                this.notPlannedGoals.removeFirst();
            }
        } else {
            setGoal = false;
        }

        return setGoal;
    }

    public void haltCurrentGoal() {
        this.haltedGoals.addLast(this.currentGoal);
        this.haltedGoals = this.sortListByPriority(this.haltedGoals);

        this.currentGoal = null;
    }

    public void updateReachedGoals() {
        this.reachedGoals.addLast(this.currentGoal);

        this.currentGoal = null;
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

    private LinkedList<GoalState> sortListByPriority(LinkedList<GoalState> list) {
        return list
                .stream()
                .sorted(Comparator.comparingInt(GoalState::getPriority))
                .collect(Collectors.toCollection(LinkedList::new));
    }
}
