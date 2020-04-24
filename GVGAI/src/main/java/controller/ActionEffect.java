/*
 * ActionEffect.java
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

public class ActionEffect {
    private String effectPredicate;
    private List<String> conditions;

    public ActionEffect(String effectPredicate, List<String> conditions) {
        this.effectPredicate = effectPredicate;
        this.conditions = conditions;
    }

    public String getEffectPredicate() {
        return this.effectPredicate;
    }

    public List<String> getConditions() {
        return this.conditions;
    }
}
