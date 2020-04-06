/*
 * PDDLSingleGoal.java
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

import java.util.List;

/**
 * Class that represents a PDDL goal. A PDDLSingleGoal object contains a PDDL predicate
 * and its priority with regard to other goals.
 *
 * @author Vladislav Nikolov Vasilev
 */
public class PDDLSingleGoal {
    private String goalPredicate;
    private int priority;
    private boolean saveGoal;
    private List<String> removeReachedGoalsList;

    /**
     * Class constructor. Creates a new PDDLSingleGoal instance with a given PDDL predicate and a priority.
     * @param goalPredicate A PDDL predicate which represents a goal.
     * @param priority The goal's priority with regard to other goals. A smaller priority
     *                 value means that the goal has more priority whereas a higher one means
     *                 that it has less priority.
     */
    public PDDLSingleGoal(String goalPredicate, int priority) {
        this.goalPredicate = goalPredicate;
        this.priority = priority;
    }

    public  PDDLSingleGoal() {}

    public void setGoalPredicate(String goalPredicate) {
        this.goalPredicate = goalPredicate;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setSaveGoal(boolean saveGoal) {
        this.saveGoal = saveGoal;
    }

    /**
     * Goal predicate getter.
     * @return Returns the PDDL goal predicate.
     */
    public String getGoalPredicate() {
        return this.goalPredicate;
    }

    /**
     * Goal priority getter.
     * @return Returns the goal's priority.
     */
    public int getPriority() {
        return this.priority;
    }

    public boolean isSaveGoal() {
        return this.saveGoal;
    }

    public List<String> getRemoveReachedGoalsList() {
        return removeReachedGoalsList;
    }

    public void setRemoveReachedGoalsList(List<String> removeReachedGoalsList) {
        this.removeReachedGoalsList = removeReachedGoalsList;
    }

    @Override
    public String toString() {
        return "PDDLSingleGoal{" +
                "goalPredicate='" + goalPredicate + '\'' +
                ", priority=" + priority +
                ", saveGoal=" + saveGoal +
                ", removeReachedGoalsList=" + removeReachedGoalsList +
                '}';
    }
}
