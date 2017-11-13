package objective.taskboard;

import java.util.Arrays;
import java.util.List;

/*-
 * [LICENSE]
 * Taskboard
 * - - -
 * Copyright (C) 2015 - 2016 Objective Solutions
 * - - -
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

public abstract class Constants {

    public static final List<String> FROMJIRA_HEADERS = Arrays.asList("project", "demand_type", "demand_status", "demand_num", "DEMAND_SUMMARY", "demand_description", "TASK_TYPE", "TASK_STATUS", "TASK_NUM", "TASK_SUMMARY", "TASK_DESCRIPTION", "TASK_FULL_DESCRIPTION", "SUBTASK_TYPE", "SUBTASK_STATUS", "SUBTASK_NUM", "SUBTASK_SUMMARY", "SUBTASK_DESCRIPTION", "SUBTASK_FULL_DESCRIPTION", "DEMAND_ID", "TASK_ID", "SUBTASK_ID", "planning_type", "task_release", "worklog", "wrong_worklog", "Demand_ballpark", "TASK_BALLPARK", "tshirt_size", "Query_Type", "EffortEstimate", "CycleEstimate", "EffortOnBacklog", "CycleOnBacklog", "BallparkEffort", "PlannedEffort", "EffortDone", "CycleDone", "WorklogDone", "WorklogDoing", "CountTasks", "CountDemands", "CountSubtasks", "SubtaskEstimativeForEEPCalculation", "PlannedEffortOnBug", "WorklogOnBug");

}
