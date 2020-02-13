/*
 * Agenda.java
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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Class that represents an agenda like data structure. The agenda is made up
 * of a list of not planned goals (those
 *
 * @author Vladislav Nikolov Vasilev
 */
public class Agenda {
    private LinkedList<PDDLGoal> notPlannedGoals;
    private LinkedList<PDDLGoal> haltedGoals;
    private LinkedList<PDDLGoal> reachedGoals;
    private PDDLGoal currentGoal;

    /**
     * Class constructor.
     *
     * @param goals List of goals
     */
    public Agenda(LinkedList<PDDLGoal> goals) {
        this.notPlannedGoals = this.sortListByPriority(goals);
        this.haltedGoals = new LinkedList<>();
        this.reachedGoals = new LinkedList<>();
        this.currentGoal = null;
    }

    public LinkedList<PDDLGoal> getNotPlannedGoals() {
        return this.notPlannedGoals;
    }

    public LinkedList<PDDLGoal> getHaltedGoals() {
        return this.haltedGoals;
    }

    public LinkedList<PDDLGoal> getReachedGoals() {
        return this.reachedGoals;
    }

    /**
     * Current goal getter.
     * @return Returns the current goal.
     */
    public PDDLGoal getCurrentGoal() {
        return this.currentGoal;
    }

    /**
     * Method that sets the current goal.
     *
     * @return Returns true if the goal has been set successfully or false if it hasn't.
     */
    public boolean setCurrentGoal() {
        boolean setGoal = true;

        if (!this.notPlannedGoals.isEmpty() && this.haltedGoals.isEmpty()) {
            this.currentGoal = this.notPlannedGoals.removeFirst();
        } else if (this.notPlannedGoals.isEmpty() && !this.haltedGoals.isEmpty()) {
            this.currentGoal = this.haltedGoals.removeFirst();
        } else if (!this.notPlannedGoals.isEmpty() && !this.haltedGoals.isEmpty()) {
            PDDLGoal firstNotPlanned = this.notPlannedGoals.getFirst(),
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

    /**
     * Method that allows to halt the current goal in case some discrepancy is found.
     */
    public void haltCurrentGoal() {
        this.haltedGoals.addLast(this.currentGoal);
        this.haltedGoals = this.sortListByPriority(this.haltedGoals);

        this.currentGoal = null;
    }

    /**
     * Method that allows to update the reached goals list. It should be called when the
     * current goal has been reached successfully.
     */
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

    private LinkedList<PDDLGoal> sortListByPriority(LinkedList<PDDLGoal> list) {
        return list
                .stream()
                .sorted(Comparator.comparingInt(PDDLGoal::getPriority))
                .collect(Collectors.toCollection(LinkedList::new));
    }
}
