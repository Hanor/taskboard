/*-
 * [LICENSE]
 * Taskboard
 * ---
 * Copyright (C) 2015 - 2017 Objective Solutions
 * ---
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * [/LICENSE]
 */

package objective.taskboard.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProjectData {
    public String projectKey;
    public String defaultTeam;
    public List<String> followUpDataHistory = new ArrayList<String>();

    public static class ProjectConfigurationData {
        public String projectKey;
        public String startDate;
        public String deliveryDate;
        public Boolean isArchived;
        public BigDecimal risk;
        public Integer projectionTimespan;
        public Long defaultTeam;
    }
}
