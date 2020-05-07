/*
 * GameInformation.java
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import ontology.Types;

/**
 * Class that contains the game's information. It is loaded a YAML configuration
 * file.
 */
public class GameInformation {
    public String domainFile;
    public String domainName;
    public Map<String, ArrayList<String>> gameElementsCorrespondence;
    public Map<Position, String> orientationCorrespondence;
    public LinkedList<PDDLSingleGoal> goals;
    public Map<String, Types.ACTIONS> actionsCorrespondence;
    public Map<Position, String> connections;
    public Map<String, String> variablesTypes;
    public String cellVariable;
    public String avatarVariable;

    public GameInformation() { }
}
