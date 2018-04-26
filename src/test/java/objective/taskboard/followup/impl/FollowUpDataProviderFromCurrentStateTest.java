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
package objective.taskboard.followup.impl;

import static java.util.Arrays.asList;
import static objective.taskboard.followup.FollowUpHelper.fromJiraRowstoString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import objective.taskboard.data.Worklog;
import objective.taskboard.followup.FromJiraDataRow;
import objective.taskboard.followup.cluster.FollowUpClusterItem;

public class FollowUpDataProviderFromCurrentStateTest extends AbstractFollowUpDataProviderTest {

    @Test
    public void demandWithoutChildFeatures_shouldCreateASingleBallpark() {
        issues( 
            demand().id(1).key("PROJ-1").summary("Smry 1").originalEstimateInHours(1).timeSpentInHours(10)
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 1\n" +
            " demandNum                     : PROJ-1\n" +
            " demandSummary                 : Smry 1\n" +
            " demandDescription             : M | 00001 - Smry 1\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 0\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : BALLPARK - Demand\n" +
            " taskStatus                    : Open\n" +
            " taskId                        : 0\n" +
            " taskNum                       : PROJ-1\n" +
            " taskSummary                   : Dummy Feature\n" +
            " taskDescription               : 00000 - Smry 1\n" +
            " taskFullDescription           : BALLPARK - Demand | M | 00000 - Smry 1\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 0\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : null\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : null\n" +
            " taskLabels                    : null\n" +
            " taskComponents                : null\n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : null\n" +
            " taskClassOfService            : null\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : null\n" +
            " taskIsBlocked                 : null\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Demand\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : Smry 1\n" +
            " subtaskDescription            : M | 00000 - Smry 1\n" +
            " subtaskFullDescription        : BALLPARK - Demand | M | 00000 - Smry 1\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : M\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 10.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : DEMAND BALLPARK"
        );

    }

    @Test
    public void demandWithRelease_shouldCreateABallparkWithReleaseInfo() {
        issues( 
            demand().id(1).key("PROJ-1").summary("Smry 1").originalEstimateInHours(1).timeSpentInHours(10).release("Release 42")
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 1\n" +
            " demandNum                     : PROJ-1\n" +
            " demandSummary                 : Smry 1\n" +
            " demandDescription             : M | 00001 - Smry 1\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 0\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : BALLPARK - Demand\n" +
            " taskStatus                    : Open\n" +
            " taskId                        : 0\n" +
            " taskNum                       : PROJ-1\n" +
            " taskSummary                   : Dummy Feature\n" +
            " taskDescription               : 00000 - Smry 1\n" +
            " taskFullDescription           : BALLPARK - Demand | M | 00000 - Smry 1\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : Release 42\n" +
            " taskStatusPriority            : 0\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : null\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : null\n" +
            " taskLabels                    : null\n" +
            " taskComponents                : null\n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : null\n" +
            " taskClassOfService            : null\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : null\n" +
            " taskIsBlocked                 : null\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Demand\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : Smry 1\n" +
            " subtaskDescription            : M | 00000 - Smry 1\n" +
            " subtaskFullDescription        : BALLPARK - Demand | M | 00000 - Smry 1\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : M\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 10.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : DEMAND BALLPARK"
        );
    }

    @Test
    public void subtasksEffortEstimateShouldNotBeLessThanFeatureEstimate() {
        configureBallparkMappings(
                taskIssueType + ":",
                "  - issueType: BALLPARK - Dev",
                "    tshirtCustomFieldId: Dev_Tshirt",
                "    jiraIssueTypes:",
                "      - " + devIssueType,
                "  - issueType: BALLPARK - Alpha",
                "    tshirtCustomFieldId: Alpha_Tshirt",
                "    jiraIssueTypes:",
                "      - " + alphaIssueType
                );
        
        configureCluster(
                new FollowUpClusterItem(projectConfiguration, "BALLPARK - Dev",   "na", "M",  5.0, 0.0),
                new FollowUpClusterItem(projectConfiguration, "BALLPARK - Alpha", "na", "XS", 1.0, 0.0),
                new FollowUpClusterItem(projectConfiguration, "Dev",              "na", "S",  2.0, 0.0)
        );

        issues( 
                task().id(3).key("PROJ-3")
                    .tshirt("Dev_Tshirt",   "M")
                    .tshirt("Alpha_Tshirt", "XS"),
                subtask().id(4).key("PROJ-20").parent("PROJ-3").issueType(devIssueType).tshirtSize("S")
        );

        //effort dev ballpark: 3, effort dev subtasks: 2, total: 5
        assertFromJiraRows(r -> asList(r.taskNum, r.subtaskNum, r.subtaskType, r.taskBallpark),
                "PROJ-3 | PROJ-0  | BALLPARK - Dev   | 3.0",
                "PROJ-3 | PROJ-0  | BALLPARK - Alpha | 1.0",
                "PROJ-3 | PROJ-20 | Dev              | 0.0"
        );
        
        issues( 
                task().id(3).key("PROJ-3")
                    .tshirt("Dev_Tshirt",   "M")
                    .tshirt("Alpha_Tshirt", "XS"),
                subtask().id(4).key("PROJ-20").parent("PROJ-3").issueType(devIssueType).tshirtSize("S"),
                subtask().id(5).key("PROJ-21").parent("PROJ-3").issueType(devIssueType).tshirtSize("S")
        );
        
        //effort dev ballpark: 1, effort dev subtasks: 4, total: 5
        assertFromJiraRows(r -> asList(r.taskNum, r.subtaskNum, r.subtaskType, r.taskBallpark),
                "PROJ-3 | PROJ-0  | BALLPARK - Dev   | 1.0",
                "PROJ-3 | PROJ-0  | BALLPARK - Alpha | 1.0",
                "PROJ-3 | PROJ-20 | Dev              | 0.0",
                "PROJ-3 | PROJ-21 | Dev              | 0.0"
        );
        
        issues( 
                task().id(3).key("PROJ-3")
                    .tshirt("Dev_Tshirt",   "M")
                    .tshirt("Alpha_Tshirt", "XS"),
                subtask().id(4).key("PROJ-20").parent("PROJ-3").issueType(devIssueType).tshirtSize("S"),
                subtask().id(5).key("PROJ-21").parent("PROJ-3").issueType(devIssueType).tshirtSize("S"),
                subtask().id(6).key("PROJ-22").parent("PROJ-3").issueType(devIssueType).tshirtSize("S")
        );
        
        //effort dev ballpark: 0, effort dev subtasks: 6, total: 6
        assertFromJiraRows(r -> asList(r.taskNum, r.subtaskNum, r.subtaskType, r.taskBallpark),
                "PROJ-3 | PROJ-0  | BALLPARK - Alpha | 1.0",
                "PROJ-3 | PROJ-20 | Dev              | 0.0",
                "PROJ-3 | PROJ-21 | Dev              | 0.0",
                "PROJ-3 | PROJ-22 | Dev              | 0.0"
        );
    }
    
    @Test
    public void subtaskWithDemandAndSubtask_shouldCreateOnlyOneSubTaskAndNoBallparks() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n"
                );
        issues( 
                demand().id(1).key("PROJ-1").summary("Smry 1").originalEstimateInHours(1).timeSpentInHours(10).priorityOrder(1l),
                demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).priorityOrder(1l),
                task().id(3).key("PROJ-3").summary("Smry 3").originalEstimateInHours(2).parent("PROJ-2").priorityOrder(1l),
                subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL").priorityOrder(1l)
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 1\n" +
            " demandNum                     : PROJ-1\n" +
            " demandSummary                 : Smry 1\n" +
            " demandDescription             : M | 00001 - Smry 1\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : BALLPARK - Demand\n" +
            " taskStatus                    : Open\n" +
            " taskId                        : 0\n" +
            " taskNum                       : PROJ-1\n" +
            " taskSummary                   : Dummy Feature\n" +
            " taskDescription               : 00000 - Smry 1\n" +
            " taskFullDescription           : BALLPARK - Demand | M | 00000 - Smry 1\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 0\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : null\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : null\n" +
            " taskLabels                    : null\n" +
            " taskComponents                : null\n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : null\n" +
            " taskClassOfService            : null\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : null\n" +
            " taskIsBlocked                 : null\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Demand\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : Smry 1\n" +
            " subtaskDescription            : M | 00000 - Smry 1\n" +
            " subtaskFullDescription        : BALLPARK - Demand | M | 00000 - Smry 1\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : M\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 10.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : DEMAND BALLPARK"+

            "\n\n"+

            " planningType                  : Plan\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 1\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : Dev\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 4\n" +
            " subtaskNum                    : PROJ-4\n" +
            " subtaskSummary                : Smry 4\n" +
            " subtaskDescription            : XL | 00004 - Smry 4\n" +
            " subtaskFullDescription        : To Do > Dev | XL | 00004 - Smry 4\n" +
            " subtaskStatusPriority         : 5\n" +
            " subtaskPriorityOrder          : 1\n" +
            " subtaskStartDateStepMillis    : 0\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " subtaskLabels                 : \n" +
            " subtaskComponents             : \n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : \n" +
            " subtaskClassOfService         : Standard\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : 1.0\n" +
            " subtaskIsBlocked              : false\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : XL\n" +
            " worklog                       : 5.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : SUBTASK PLAN");
    }
    
    @Test
    public void featureWithTwoBallparksMappedToTheSameIssueTypes_ShouldNotGenerateBallParks() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n" +
                "\n" +
                "  - issueType : BALLPARK - Front Development\n" +
                "    tshirtCustomFieldId: FrontDev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n"
                );
        issues( 
                demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).priorityOrder(1l),
                task().id(3).key("PROJ-3").summary("Smry 3").parent("PROJ-2").priorityOrder(1l)
                    .tshirt("Dev_Tshirt",      "S")
                    .tshirt("FrontDev_Tshirt", "S"),
                subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL").priorityOrder(1l)
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Plan\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 1\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : Dev\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 4\n" +
            " subtaskNum                    : PROJ-4\n" +
            " subtaskSummary                : Smry 4\n" +
            " subtaskDescription            : XL | 00004 - Smry 4\n" +
            " subtaskFullDescription        : To Do > Dev | XL | 00004 - Smry 4\n" +
            " subtaskStatusPriority         : 5\n" +
            " subtaskPriorityOrder          : 1\n" +
            " subtaskStartDateStepMillis    : 0\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " subtaskLabels                 : \n" +
            " subtaskComponents             : \n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : \n" +
            " subtaskClassOfService         : Standard\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : 1.0\n" +
            " subtaskIsBlocked              : false\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : XL\n" +
            " worklog                       : 5.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : SUBTASK PLAN");
    }

    @Test
    public void featureWithoutAnySubtasks_ShouldCreateDummyFeaturesForAllTShirts() {
        configureBallparkMappings(
            taskIssueType + " : \n" +
            "  - issueType : BALLPARK - Development\n" +
            "    tshirtCustomFieldId: Dev_Tshirt\n" +
            "    jiraIssueTypes:\n" +
            "      - "+ devIssueType + "\n" +

            "  - issueType : BALLPARK - Alpha\n" +
            "    tshirtCustomFieldId: Alpha_TestTshirt\n" +
            "    jiraIssueTypes:\n" +
            "      - " + alphaIssueType + " # UX\n");

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt"));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).priorityOrder(1l),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                .tshirt("Dev_Tshirt","L")
                .tshirt("Alpha_TestTshirt","S")
                .priorityOrder(1l)
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 1\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Development\n" +
            " subtaskStatus                 : Open\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : BALLPARK - Development\n" +
            " subtaskDescription            : 00000 - Smry 3\n" +
            " subtaskFullDescription        : BALLPARK - Development | 00000 - Smry 3\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : L\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 1.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 2.0\n" +
            " queryType                     : FEATURE BALLPARK"+

            "\n\n"+

            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 1\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Alpha\n" +
            " subtaskStatus                 : Open\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : BALLPARK - Alpha\n" +
            " subtaskDescription            : 00000 - Smry 3\n" +
            " subtaskFullDescription        : BALLPARK - Alpha | 00000 - Smry 3\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : S\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 1.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 2.0\n" +
            " queryType                     : FEATURE BALLPARK");
    }
    
    @Test
    public void featureWithOneSubtask_ShouldCreateOnlyDummyFeatureForMissingSubtask() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - "+ frontEndIssueType + "\n" +
                "      - "+ devIssueType + "\n" +

                "  - issueType : BALLPARK - Alpha\n" +
                "    tshirtCustomFieldId: Alpha_TestTshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + alphaIssueType + "\n" +

                "  - issueType : BALLPARK - Deploy\n" +
                "    tshirtCustomFieldId: Deploy_TestTshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + deployIssueType + "\n" +

                reviewIssueType + " : \n" +
                "  - issueType : BALLPARK - Review\n" +
                "    tshirtCustomFieldId: Review_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - "+ devIssueType + "\n"
                );

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt","Review_Tshirt"));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).priorityOrder(1l),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").timeSpentInHours(1)
                    .tshirt("Dev_Tshirt", "L")
                    .tshirt("Alpha_TestTshirt", "S")
                    .priorityOrder(1l),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL").priorityOrder(1l)
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 1\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Alpha\n" +
            " subtaskStatus                 : Open\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : BALLPARK - Alpha\n" +
            " subtaskDescription            : 00000 - Smry 3\n" +
            " subtaskFullDescription        : BALLPARK - Alpha | 00000 - Smry 3\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : S\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 1.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : FEATURE BALLPARK" +

            "\n\n"+

            " planningType                  : Plan\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 1\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : Dev\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 4\n" +
            " subtaskNum                    : PROJ-4\n" +
            " subtaskSummary                : Smry 4\n" +
            " subtaskDescription            : XL | 00004 - Smry 4\n" +
            " subtaskFullDescription        : To Do > Dev | XL | 00004 - Smry 4\n" +
            " subtaskStatusPriority         : 5\n" +
            " subtaskPriorityOrder          : 1\n" +
            " subtaskStartDateStepMillis    : 0\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " subtaskLabels                 : \n" +
            " subtaskComponents             : \n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : \n" +
            " subtaskClassOfService         : Standard\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : 1.0\n" +
            " subtaskIsBlocked              : false\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : XL\n" +
            " worklog                       : 5.0\n" +
            " wrongWorklog                  : 1.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : SUBTASK PLAN");
    }

    @Test
    public void featureWithAllSubtasks_ShouldNotCreateDummies() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n" +

                "  - issueType : BALLPARK - Alpha\n" +
                "    tshirtCustomFieldId: Alpha_TestTshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + alphaIssueType + "\n"
                );

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt","Review_Tshirt"));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).priorityOrder(1l),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3")
                    .tshirt("Dev_Tshirt", "L")
                    .tshirt("Alpha_TestTshirt", "S")
                    .priorityOrder(1l),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL").priorityOrder(1l),

            subtask().id(5).key("PROJ-5").summary("Smry 5").timeSpentInHours(15).parent("PROJ-3").issueType(alphaIssueType).tshirtSize("L").priorityOrder(1l)
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Plan\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 1\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : Dev\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 4\n" +
            " subtaskNum                    : PROJ-4\n" +
            " subtaskSummary                : Smry 4\n" +
            " subtaskDescription            : XL | 00004 - Smry 4\n" +
            " subtaskFullDescription        : To Do > Dev | XL | 00004 - Smry 4\n" +
            " subtaskStatusPriority         : 5\n" +
            " subtaskPriorityOrder          : 1\n" +
            " subtaskStartDateStepMillis    : 0\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " subtaskLabels                 : \n" +
            " subtaskComponents             : \n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : \n" +
            " subtaskClassOfService         : Standard\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : 1.0\n" +
            " subtaskIsBlocked              : false\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : XL\n" +
            " worklog                       : 5.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : SUBTASK PLAN" +

            "\n\n"+

            " planningType                  : Plan\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 1\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : Alpha\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 5\n" +
            " subtaskNum                    : PROJ-5\n" +
            " subtaskSummary                : Smry 5\n" +
            " subtaskDescription            : L | 00005 - Smry 5\n" +
            " subtaskFullDescription        : To Do > Alpha | L | 00005 - Smry 5\n" +
            " subtaskStatusPriority         : 5\n" +
            " subtaskPriorityOrder          : 1\n" +
            " subtaskStartDateStepMillis    : 0\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " subtaskLabels                 : \n" +
            " subtaskComponents             : \n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : \n" +
            " subtaskClassOfService         : Standard\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : 1.0\n" +
            " subtaskIsBlocked              : false\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : L\n" +
            " worklog                       : 15.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : SUBTASK PLAN");
    }

    @Test
    public void featureWithRelease_ShouldCreateBallparksWithRelease() {
        configureBallparkMappings(
            taskIssueType + " : \n" +
            "  - issueType : BALLPARK - Development\n" +
            "    tshirtCustomFieldId: Dev_Tshirt\n" +
            "    jiraIssueTypes:\n" +
            "      - "+ devIssueType + "\n");

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt"));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                .tshirt("Dev_Tshirt","L").release("release 66")
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 0\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : release 66\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Development\n" +
            " subtaskStatus                 : Open\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : BALLPARK - Development\n" +
            " subtaskDescription            : 00000 - Smry 3\n" +
            " subtaskFullDescription        : BALLPARK - Development | 00000 - Smry 3\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : L\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 1.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 2.0\n" +
            " queryType                     : FEATURE BALLPARK"
            );
    }

    @Test
    public void featureWithRelease_ShouldCreateSubtaskWithRelease() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n"
                );

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt","Review_Tshirt"));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3")
                    .tshirt("Dev_Tshirt", "L").release("release 66"),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL")
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Plan\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 0\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : release 66\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : Dev\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 4\n" +
            " subtaskNum                    : PROJ-4\n" +
            " subtaskSummary                : Smry 4\n" +
            " subtaskDescription            : XL | 00004 - Smry 4\n" +
            " subtaskFullDescription        : To Do > Dev | XL | 00004 - Smry 4\n" +
            " subtaskStatusPriority         : 5\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : 0\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " subtaskLabels                 : \n" +
            " subtaskComponents             : \n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : \n" +
            " subtaskClassOfService         : Standard\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : 1.0\n" +
            " subtaskIsBlocked              : false\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : XL\n" +
            " worklog                       : 5.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : SUBTASK PLAN"
            );
    }

    @Test
    public void subtaskWithoutParents_shouldSubTaskWithNullFields() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n"
                );
        issues( 
                task().id(3).key("PROJ-3").summary("Smry 3").originalEstimateInHours(2),
                subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL")
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Plan\n" +
            " project                       : A Project\n" +
            " demandType                    : \n" +
            " demandStatus                  : \n" +
            " demandId                      : \n" +
            " demandNum                     : \n" +
            " demandSummary                 : \n" +
            " demandDescription             : \n" +
            " demandStatusPriority          : 0\n" +
            " demandPriorityOrder           : 0\n" +
            " demandStartDateStepMillis     : null\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : null\n" +
            " demandLabels                  : null\n" +
            " demandComponents              : null\n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : null\n" +
            " demandClassOfService          : null\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : null\n" +
            " demandIsBlocked               : null\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : Dev\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 4\n" +
            " subtaskNum                    : PROJ-4\n" +
            " subtaskSummary                : Smry 4\n" +
            " subtaskDescription            : XL | 00004 - Smry 4\n" +
            " subtaskFullDescription        : To Do > Dev | XL | 00004 - Smry 4\n" +
            " subtaskStatusPriority         : 5\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : 0\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " subtaskLabels                 : \n" +
            " subtaskComponents             : \n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : \n" +
            " subtaskClassOfService         : Standard\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : 1.0\n" +
            " subtaskIsBlocked              : false\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : XL\n" +
            " worklog                       : 5.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 0.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : SUBTASK PLAN");
        }

    @Test
    public void featureWithCertainStatus_ShouldNotGenerateBallparks() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - "+ frontEndIssueType + "\n" +
                "      - "+ devIssueType + "\n" 
                );

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt"));
        followup.setFeatureStatusThatDontGenerateBallpark(asList(statusDone));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                    .tshirt("Dev_Tshirt", "L").issueStatus(statusDone)
        );

        assertFollowupsForIssuesEquals("");
    }

    @Test
    public void subtaskWithCertainStatus_ShouldNotPreventBallparkGeneration() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - "+ devIssueType + "\n"
                );

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt","Review_Tshirt"));
        followup.setSubtaskStatusThatDontPreventBallparkGeneration(asList(statusCancelled));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).priorityOrder(1l),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3")
                    .tshirt("Dev_Tshirt", "L").priorityOrder(1l),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL")
                .issueStatus(statusCancelled).priorityOrder(1l)
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 1\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Development\n" +
            " subtaskStatus                 : Open\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : BALLPARK - Development\n" +
            " subtaskDescription            : 00000 - Smry 3\n" +
            " subtaskFullDescription        : BALLPARK - Development | 00000 - Smry 3\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : L\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : FEATURE BALLPARK"+

            "\n\n"+

            " planningType                  : Plan\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 1\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : Dev\n" +
            " subtaskStatus                 : Cancelled\n" +
            " subtaskId                     : 4\n" +
            " subtaskNum                    : PROJ-4\n" +
            " subtaskSummary                : Smry 4\n" +
            " subtaskDescription            : XL | 00004 - Smry 4\n" +
            " subtaskFullDescription        : Cancelled > Dev | XL | 00004 - Smry 4\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 1\n" +
            " subtaskStartDateStepMillis    : 0\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " subtaskLabels                 : \n" +
            " subtaskComponents             : \n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : \n" +
            " subtaskClassOfService         : Standard\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : 1.0\n" +
            " subtaskIsBlocked              : false\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : XL\n" +
            " worklog                       : 5.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : SUBTASK PLAN");
    }

    @Test
    public void withoutBallparkConfiguration_ShouldFail() {
        issues( 
                task().id(3).key("PROJ-3").summary("Smry 3").originalEstimateInHours(2),
                subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL")
        );

        try {
            subject.generate(ZoneId.systemDefault(), DEFAULT_PROJECT);
            fail("Should fail when trying to generate ballbark without mapping for given issue type");
        } catch(IllegalStateException e) {
            assertEquals("Ballpark mapping for issue type 'Task' (id 12) missing in configuration", e.getMessage());
        }
    }

    @Test
    public void demandAndfeatureWithRelease_ShouldCreateBallparksWithFeatureRelease() {
        configureBallparkMappings(
            taskIssueType + " : \n" +
            "  - issueType : BALLPARK - Development\n" +
            "    tshirtCustomFieldId: Dev_Tshirt\n" +
            "    jiraIssueTypes:\n" +
            "      - "+ devIssueType + "\n");

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt"));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).release("release 66"),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                .tshirt("Dev_Tshirt","L").release("release 88")
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 0\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : release 88\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Development\n" +
            " subtaskStatus                 : Open\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : BALLPARK - Development\n" +
            " subtaskDescription            : 00000 - Smry 3\n" +
            " subtaskFullDescription        : BALLPARK - Development | 00000 - Smry 3\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : L\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 1.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 2.0\n" +
            " queryType                     : FEATURE BALLPARK"
            );
    }

    @Test
    public void demandWithReleaseAndfeatureWithout_ShouldCreateBallparksWithDemandRelease() {
        configureBallparkMappings(
            taskIssueType + " : \n" +
            "  - issueType : BALLPARK - Development\n" +
            "    tshirtCustomFieldId: Dev_Tshirt\n" +
            "    jiraIssueTypes:\n" +
            "      - "+ devIssueType + "\n");

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt"));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).release("release 66"),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                .tshirt("Dev_Tshirt","L")
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 0\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : release 66\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Development\n" +
            " subtaskStatus                 : Open\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : BALLPARK - Development\n" +
            " subtaskDescription            : 00000 - Smry 3\n" +
            " subtaskFullDescription        : BALLPARK - Development | 00000 - Smry 3\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : L\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 1.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 2.0\n" +
            " queryType                     : FEATURE BALLPARK"
            );
    }

    @Test
    public void ifSubtaskHasRelease_ShouldPreferSubtaskRelease() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n"
                );

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt","Review_Tshirt"));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).release("Demand Release #1").priorityOrder(1l),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3")
                    .tshirt("Dev_Tshirt", "L")
                    .tshirt("Alpha_TestTshirt", "S")
                    .priorityOrder(1l)
                    .release("Feature Release #2"),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL")
                    .release("Sub Task Release #3").priorityOrder(1l),

            subtask().id(5).key("PROJ-5").summary("Smry 5").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL")
                    .release("Sub Task Release #4").priorityOrder(1l)                  
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Plan\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : Sub Task Release #3\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 1\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : Dev\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 4\n" +
            " subtaskNum                    : PROJ-4\n" +
            " subtaskSummary                : Smry 4\n" +
            " subtaskDescription            : XL | 00004 - Smry 4\n" +
            " subtaskFullDescription        : To Do > Dev | XL | 00004 - Smry 4\n" +
            " subtaskStatusPriority         : 5\n" +
            " subtaskPriorityOrder          : 1\n" +
            " subtaskStartDateStepMillis    : 0\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " subtaskLabels                 : \n" +
            " subtaskComponents             : \n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : \n" +
            " subtaskClassOfService         : Standard\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : 1.0\n" +
            " subtaskIsBlocked              : false\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : XL\n" +
            " worklog                       : 5.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : SUBTASK PLAN" +

            "\n\n"+

            " planningType                  : Plan\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : Sub Task Release #4\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 1\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : Dev\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 5\n" +
            " subtaskNum                    : PROJ-5\n" +
            " subtaskSummary                : Smry 5\n" +
            " subtaskDescription            : XL | 00005 - Smry 5\n" +
            " subtaskFullDescription        : To Do > Dev | XL | 00005 - Smry 5\n" +
            " subtaskStatusPriority         : 5\n" +
            " subtaskPriorityOrder          : 1\n" +
            " subtaskStartDateStepMillis    : 0\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " subtaskLabels                 : \n" +
            " subtaskComponents             : \n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : \n" +
            " subtaskClassOfService         : Standard\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : 1.0\n" +
            " subtaskIsBlocked              : false\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : XL\n" +
            " worklog                       : 5.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : SUBTASK PLAN"
            );
    }

    @Test
    public void ifDemandHasReleaseAndFeatureAndSubTaskDoesnt_UseDemandRelease() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n"
                );

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt","Review_Tshirt"));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).release("Demand Release #1"),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3")
                    .tshirt("Dev_Tshirt", "L")
                    .tshirt("Alpha_TestTshirt", "S"),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL")
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Plan\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 0\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : Demand Release #1\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : Dev\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 4\n" +
            " subtaskNum                    : PROJ-4\n" +
            " subtaskSummary                : Smry 4\n" +
            " subtaskDescription            : XL | 00004 - Smry 4\n" +
            " subtaskFullDescription        : To Do > Dev | XL | 00004 - Smry 4\n" +
            " subtaskStatusPriority         : 5\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : 0\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " subtaskLabels                 : \n" +
            " subtaskComponents             : \n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : \n" +
            " subtaskClassOfService         : Standard\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : 1.0\n" +
            " subtaskIsBlocked              : false\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : XL\n" +
            " worklog                       : 5.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : SUBTASK PLAN"
            );
    }

    @Test
    public void verifyDemandsOrder_ShouldBeOrderedByStatusAndPriority() {
        Long hightPriority = 1l;
        Long lowPriority = 2l;
        issues(demand().id(1).key("PROJ-1").summary("Smry 1").issueStatus(statusOpen).priorityOrder(hightPriority),
                demand().id(2).key("PROJ-2").summary("Smry 2").issueStatus(statusToDo).priorityOrder(lowPriority),
                demand().id(3).key("PROJ-3").summary("Smry 3").issueStatus(statusDone).priorityOrder(hightPriority),
                demand().id(4).key("PROJ-4").summary("Smry 4").issueStatus(statusToDo).priorityOrder(hightPriority));

        assertFollowupsForIssuesEqualsOrdered(
            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : Done\n" +
            " demandId                      : 3\n" +
            " demandNum                     : PROJ-3\n" +
            " demandSummary                 : Smry 3\n" +
            " demandDescription             : M | 00003 - Smry 3\n" +
            " demandStatusPriority          : 1\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : BALLPARK - Demand\n" +
            " taskStatus                    : Open\n" +
            " taskId                        : 0\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Dummy Feature\n" +
            " taskDescription               : 00000 - Smry 3\n" +
            " taskFullDescription           : BALLPARK - Demand | M | 00000 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 0\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : null\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : null\n" +
            " taskLabels                    : null\n" +
            " taskComponents                : null\n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : null\n" +
            " taskClassOfService            : null\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : null\n" +
            " taskIsBlocked                 : null\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Demand\n" +
            " subtaskStatus                 : Done\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : Smry 3\n" +
            " subtaskDescription            : M | 00000 - Smry 3\n" +
            " subtaskFullDescription        : BALLPARK - Demand | M | 00000 - Smry 3\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : M\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 0.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : DEMAND BALLPARK" +

            "\n\n" +

            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 4\n" +
            " demandNum                     : PROJ-4\n" +
            " demandSummary                 : Smry 4\n" +
            " demandDescription             : M | 00004 - Smry 4\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : BALLPARK - Demand\n" +
            " taskStatus                    : Open\n" +
            " taskId                        : 0\n" +
            " taskNum                       : PROJ-4\n" +
            " taskSummary                   : Dummy Feature\n" +
            " taskDescription               : 00000 - Smry 4\n" +
            " taskFullDescription           : BALLPARK - Demand | M | 00000 - Smry 4\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 0\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : null\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : null\n" +
            " taskLabels                    : null\n" +
            " taskComponents                : null\n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : null\n" +
            " taskClassOfService            : null\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : null\n" +
            " taskIsBlocked                 : null\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Demand\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : Smry 4\n" +
            " subtaskDescription            : M | 00000 - Smry 4\n" +
            " subtaskFullDescription        : BALLPARK - Demand | M | 00000 - Smry 4\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : M\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 0.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : DEMAND BALLPARK" +

            "\n\n" +

            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : M | 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 2\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : BALLPARK - Demand\n" +
            " taskStatus                    : Open\n" +
            " taskId                        : 0\n" +
            " taskNum                       : PROJ-2\n" +
            " taskSummary                   : Dummy Feature\n" +
            " taskDescription               : 00000 - Smry 2\n" +
            " taskFullDescription           : BALLPARK - Demand | M | 00000 - Smry 2\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 0\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : null\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : null\n" +
            " taskLabels                    : null\n" +
            " taskComponents                : null\n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : null\n" +
            " taskClassOfService            : null\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : null\n" +
            " taskIsBlocked                 : null\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Demand\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : Smry 2\n" +
            " subtaskDescription            : M | 00000 - Smry 2\n" +
            " subtaskFullDescription        : BALLPARK - Demand | M | 00000 - Smry 2\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : M\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 0.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : DEMAND BALLPARK" +

            "\n\n" +

            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : Open\n" +
            " demandId                      : 1\n" +
            " demandNum                     : PROJ-1\n" +
            " demandSummary                 : Smry 1\n" +
            " demandDescription             : M | 00001 - Smry 1\n" +
            " demandStatusPriority          : 6\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : BALLPARK - Demand\n" +
            " taskStatus                    : Open\n" +
            " taskId                        : 0\n" +
            " taskNum                       : PROJ-1\n" +
            " taskSummary                   : Dummy Feature\n" +
            " taskDescription               : 00000 - Smry 1\n" +
            " taskFullDescription           : BALLPARK - Demand | M | 00000 - Smry 1\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 0\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : null\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : null\n" +
            " taskLabels                    : null\n" +
            " taskComponents                : null\n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : null\n" +
            " taskClassOfService            : null\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : null\n" +
            " taskIsBlocked                 : null\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Demand\n" +
            " subtaskStatus                 : Open\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : Smry 1\n" +
            " subtaskDescription            : M | 00000 - Smry 1\n" +
            " subtaskFullDescription        : BALLPARK - Demand | M | 00000 - Smry 1\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : M\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 0.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : DEMAND BALLPARK"
        );
    }

    @Test
    public void issuesInCertainStatus_shouldNotShowUpAtAll() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n"
                );

        followup.setStatusExcludedFromFollowup(asList(statusOpen));

        issues( 
                demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).release("Demand Release #1").issueStatus(statusOpen),

                task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                        .issueStatus(statusOpen)
                        .tshirt("Dev_Tshirt", "L")
                        .tshirt("Alpha_TestTshirt", "S"),

                subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL")
                        .issueStatus(statusOpen)
            );

        assertFollowupsForIssuesEquals("");
    }

    @Test
    public void issuesWithoutParent_shouldNotBreakTheFollowupGeneration() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n"
                );

        issues( 
                subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).issueType(devIssueType).tshirtSize("XL")
            );
        assertFollowupsForIssuesEquals("");
    }

    @Test
    public void nullTimeSpent_ShouldNotBreak() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - "+ frontEndIssueType + "\n" +
                "      - "+ devIssueType + "\n" 
                );

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt"));
        followup.setFeatureStatusThatDontGenerateBallpark(asList(statusDone));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2"),
            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3")
                .issueStatus(statusOpen)
                .tshirt("Dev_Tshirt", "L")
                .tshirt("Alpha_TestTshirt", "S"),
                subtask().id(4).key("PROJ-4").summary("Smry 4").parent("PROJ-3").issueType(devIssueType).tshirtSize("XL")
                .issueStatus(statusOpen)
        );

        subject.generate(ZoneId.systemDefault(), DEFAULT_PROJECT);
    }

    @Test
    public void nullOriginalEstimate_ShouldNotBreak() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - "+ frontEndIssueType + "\n" +
                "      - "+ devIssueType + "\n" 
                );

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt"));
        followup.setFeatureStatusThatDontGenerateBallpark(asList(statusDone));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").timeSpentInHours(0).originalEstimateInHours(null)
        );

        subject.generate(ZoneId.systemDefault(), DEFAULT_PROJECT);
    }

    @Test
    public void featureWithoutDemand_ShouldNotBreak() {
        configureBallparkMappings(
            taskIssueType + " : \n" +
            "  - issueType : BALLPARK - Development\n" +
            "    tshirtCustomFieldId: Dev_Tshirt\n" +
            "    jiraIssueTypes:\n" +
            "      - "+ devIssueType + "\n");

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt"));

        issues( 
            task()  .id(3).key("PROJ-3").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                .tshirt("Dev_Tshirt","L")
        );
        assertFollowupsForIssuesEquals(
            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : \n" +
            " demandStatus                  : \n" +
            " demandId                      : \n" +
            " demandNum                     : \n" +
            " demandSummary                 : \n" +
            " demandDescription             : \n" +
            " demandStatusPriority          : 0\n" +
            " demandPriorityOrder           : 0\n" +
            " demandStartDateStepMillis     : null\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : null\n" +
            " demandLabels                  : null\n" +
            " demandComponents              : null\n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : null\n" +
            " demandClassOfService          : null\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : null\n" +
            " demandIsBlocked               : null\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : 0\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " taskLabels                    : \n" +
            " taskComponents                : \n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : \n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Development\n" +
            " subtaskStatus                 : Open\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : BALLPARK - Development\n" +
            " subtaskDescription            : 00000 - Smry 3\n" +
            " subtaskFullDescription        : BALLPARK - Development | 00000 - Smry 3\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : L\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 1.0\n" +
            " demandBallpark                : 0.0\n" +
            " taskBallpark                  : 2.0\n" +
            " queryType                     : FEATURE BALLPARK");
    }

    @Test
    public void issuesOfDifferentProjects_ShouldOnlyIncludeOfSelectectedProjects() {
        issues( 
                demand().id(1).key("PROJ-1").summary("Smry 1").originalEstimateInHours(1).timeSpentInHours(10),
                demand().id(2).project("ANOTHER").key("ANOTHER-1").summary("Smry 2").originalEstimateInHours(1).timeSpentInHours(10)
            );

        assertFollowupsForIssuesEquals(
            " planningType                  : Ballpark\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 1\n" +
            " demandNum                     : PROJ-1\n" +
            " demandSummary                 : Smry 1\n" +
            " demandDescription             : M | 00001 - Smry 1\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 0\n" +
            " demandStartDateStepMillis     : 0\n" +
            " demandAssignee                : null\n" +
            " demandDueDate                 : null\n" +
            " demandCreated                 : 1969-12-31T21:00-03:00[America/Sao_Paulo]\n" +
            " demandLabels                  : \n" +
            " demandComponents              : \n" +
            " demandReporter                : null\n" +
            " demandCoAssignees             : \n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : null\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : null\n" +
            " taskType                      : BALLPARK - Demand\n" +
            " taskStatus                    : Open\n" +
            " taskId                        : 0\n" +
            " taskNum                       : PROJ-1\n" +
            " taskSummary                   : Dummy Feature\n" +
            " taskDescription               : 00000 - Smry 1\n" +
            " taskFullDescription           : BALLPARK - Demand | M | 00000 - Smry 1\n" +
            " taskAdditionalEstimatedHours  : null\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 0\n" +
            " taskPriorityOrder             : 0\n" +
            " taskStartDateStepMillis       : null\n" +
            " taskAssignee                  : null\n" +
            " taskDueDate                   : null\n" +
            " taskCreated                   : null\n" +
            " taskLabels                    : null\n" +
            " taskComponents                : null\n" +
            " taskReporter                  : null\n" +
            " taskCoAssignees               : null\n" +
            " taskClassOfService            : null\n" +
            " taskUpdatedDate               : null\n" +
            " taskCycletime                 : null\n" +
            " taskIsBlocked                 : null\n" +
            " taskLastBlockReason           : null\n" +
            " subtaskType                   : BALLPARK - Demand\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 0\n" +
            " subtaskNum                    : PROJ-0\n" +
            " subtaskSummary                : Smry 1\n" +
            " subtaskDescription            : M | 00000 - Smry 1\n" +
            " subtaskFullDescription        : BALLPARK - Demand | M | 00000 - Smry 1\n" +
            " subtaskStatusPriority         : 0\n" +
            " subtaskPriorityOrder          : 0\n" +
            " subtaskStartDateStepMillis    : null\n" +
            " subtaskAssignee               : null\n" +
            " subtaskDueDate                : null\n" +
            " subtaskCreated                : null\n" +
            " subtaskLabels                 : null\n" +
            " subtaskComponents             : null\n" +
            " subtaskReporter               : null\n" +
            " subtaskCoAssignees            : null\n" +
            " subtaskClassOfService         : null\n" +
            " subtaskUpdatedDate            : null\n" +
            " subtaskCycletime              : null\n" +
            " subtaskIsBlocked              : null\n" +
            " subtaskLastBlockReason        : null\n" +
            " tshirtSize                    : M\n" +
            " worklog                       : 0.0\n" +
            " wrongWorklog                  : 10.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : DEMAND BALLPARK"
        );
    }

    @Test
    public void whenAllFieldsHaveValues_emptyAndNullAreNotAllowed() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n" +

                "  - issueType : BALLPARK - Alpha\n" +
                "    tshirtCustomFieldId: Alpha_TestTshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + alphaIssueType + "\n"
                );

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt","Review_Tshirt"));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2")
                    .assignee("demand.assignee").coAssignees("demand.coassignee.1", "demand.coassignee.2").reporter("demand.reporter")
                    .labels("demand-label-1","demand-label-2").components("demand-component-1","demand-component-2")
                    .created("2016-10-26").priorityUpdatedDate("2018-11-27").dueDate("2020-12-28").originalEstimateInHours(1).priorityOrder(1l)
                    .startDateStepMillis(1513101243000L).additionalEstimatedHours(80D).lastBlockReason("Demand Last BLock Reason"),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3")
                    .assignee("task.assignee").coAssignees("task.coassignee.1", "task.coassignee.2").reporter("task.reporter")
                    .labels("task-label-1","task-label-2").components("task-component-1","task-component-2")
                    .created("2016-10-26").priorityUpdatedDate("2018-11-27").dueDate("2020-12-28").originalEstimateInHours(0)
                    .tshirt("Dev_Tshirt", "L").priorityOrder(1l)
                    .startDateStepMillis(1513101243000L).additionalEstimatedHours(80D).lastBlockReason("Task Last BLock Reason"),

            subtask().id(4).key("PROJ-4").parent("PROJ-3").summary("Smry 4").isBlocked("Yes")
                    .assignee("subtask.assignee").coAssignees("subtask.coassignee.1", "subtask.coassignee.2").reporter("subtask.reporter")
                    .labels("subtask-label-1","subtask-label-2").components("subtask-component-1","subtask-component-2")
                    .created("2016-10-26").priorityUpdatedDate("2018-11-27").dueDate("2020-12-28")
                    .timeSpentInHours(5).issueType(devIssueType).tshirtSize("XL").priorityOrder(1l).originalEstimateInHours(7)
                    .startDateStepMillis(1513101243000L).additionalEstimatedHours(80D).lastBlockReason("Subtask Last BLock Reason")
        );

        assertFollowupsForIssuesEquals(
            " planningType                  : Plan\n" +
            " project                       : A Project\n" +
            " demandType                    : Demand\n" +
            " demandStatus                  : To Do\n" +
            " demandId                      : 2\n" +
            " demandNum                     : PROJ-2\n" +
            " demandSummary                 : Smry 2\n" +
            " demandDescription             : 00002 - Smry 2\n" +
            " demandStatusPriority          : 5\n" +
            " demandPriorityOrder           : 1\n" +
            " demandStartDateStepMillis     : 1513101243000\n" +
            " demandAssignee                : demand.assignee\n" +
            " demandDueDate                 : 2020-12-28T00:00-02:00[America/Sao_Paulo]\n" +
            " demandCreated                 : 2016-10-26T00:00-02:00[America/Sao_Paulo]\n" +
            " demandLabels                  : demand-label-1,demand-label-2\n" +
            " demandComponents              : demand-component-1,demand-component-2\n" +
            " demandReporter                : demand.reporter\n" +
            " demandCoAssignees             : demand.coassignee.1,demand.coassignee.2\n" +
            " demandClassOfService          : Standard\n" +
            " demandUpdatedDate             : 2018-11-27T00:00-02:00[America/Sao_Paulo]\n" +
            " demandCycletime               : 1.0\n" +
            " demandIsBlocked               : false\n" +
            " demandLastBlockReason         : Demand Last BLock Reason\n" +
            " taskType                      : Task\n" +
            " taskStatus                    : To Do\n" +
            " taskId                        : 3\n" +
            " taskNum                       : PROJ-3\n" +
            " taskSummary                   : Smry 3\n" +
            " taskDescription               : 00003 - Smry 3\n" +
            " taskFullDescription           : Task | 00003 - Smry 3\n" +
            " taskAdditionalEstimatedHours  : 80.0\n" +
            " taskRelease                   : No release set\n" +
            " taskStatusPriority            : 9\n" +
            " taskPriorityOrder             : 1\n" +
            " taskStartDateStepMillis       : 1513101243000\n" +
            " taskAssignee                  : task.assignee\n" +
            " taskDueDate                   : 2020-12-28T00:00-02:00[America/Sao_Paulo]\n" +
            " taskCreated                   : 2016-10-26T00:00-02:00[America/Sao_Paulo]\n" +
            " taskLabels                    : task-label-1,task-label-2\n" +
            " taskComponents                : task-component-1,task-component-2\n" +
            " taskReporter                  : task.reporter\n" +
            " taskCoAssignees               : task.coassignee.1,task.coassignee.2\n" +
            " taskClassOfService            : Standard\n" +
            " taskUpdatedDate               : 2018-11-27T00:00-02:00[America/Sao_Paulo]\n" +
            " taskCycletime                 : 1.0\n" +
            " taskIsBlocked                 : false\n" +
            " taskLastBlockReason           : Task Last BLock Reason\n" +
            " subtaskType                   : Dev\n" +
            " subtaskStatus                 : To Do\n" +
            " subtaskId                     : 4\n" +
            " subtaskNum                    : PROJ-4\n" +
            " subtaskSummary                : Smry 4\n" +
            " subtaskDescription            : XL | 00004 - Smry 4\n" +
            " subtaskFullDescription        : To Do > Dev | XL | 00004 - Smry 4\n" +
            " subtaskStatusPriority         : 5\n" +
            " subtaskPriorityOrder          : 1\n" +
            " subtaskStartDateStepMillis    : 1513101243000\n" +
            " subtaskAssignee               : subtask.assignee\n" +
            " subtaskDueDate                : 2020-12-28T00:00-02:00[America/Sao_Paulo]\n" +
            " subtaskCreated                : 2016-10-26T00:00-02:00[America/Sao_Paulo]\n" +
            " subtaskLabels                 : subtask-label-1,subtask-label-2\n" +
            " subtaskComponents             : subtask-component-1,subtask-component-2\n" +
            " subtaskReporter               : subtask.reporter\n" +
            " subtaskCoAssignees            : subtask.coassignee.1,subtask.coassignee.2\n" +
            " subtaskClassOfService         : Standard\n" +
            " subtaskUpdatedDate            : 2018-11-27T00:00-02:00[America/Sao_Paulo]\n" +
            " subtaskCycletime              : 1.0\n" +
            " subtaskIsBlocked              : true\n" +
            " subtaskLastBlockReason        : Subtask Last BLock Reason\n" +
            " tshirtSize                    : XL\n" +
            " worklog                       : 5.0\n" +
            " wrongWorklog                  : 0.0\n" +
            " demandBallpark                : 1.0\n" +
            " taskBallpark                  : 0.0\n" +
            " queryType                     : SUBTASK PLAN");
    }
    
    @Test
    public void whenThereAreWrongWorklogInDemand_ShouldShowupInFeatureBallparks() {
        configureBallparkMappings(
            taskIssueType + " : \n" +
            "  - issueType : BALLPARK - Development\n" +
            "    tshirtCustomFieldId: Dev_Tshirt\n" +
            "    jiraIssueTypes:\n" +
            "      - "+ devIssueType + "\n" +

            "  - issueType : BALLPARK - Alpha\n" +
            "    tshirtCustomFieldId: Alpha_TestTshirt\n" +
            "    jiraIssueTypes:\n" +
            "      - " + alphaIssueType + " # UX\n");

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt"));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).timeSpentInHours(10).priorityOrder(1l),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                .tshirt("Dev_Tshirt","L")
                .tshirt("Alpha_TestTshirt","S")
                .priorityOrder(1l)
        );

        List<FromJiraDataRow> rows = subject.generate(ZoneId.systemDefault(), DEFAULT_PROJECT).fromJiraDs.rows;
        assertEquals((Double)11.0, rows.get(0).wrongWorklog);
        assertEquals((Double)11.0, rows.get(1).wrongWorklog);
    }
    
    @Test
    public void ifFeatureHasWorklog_SubtaskShouldHaveWrongWorklog() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n" +

                "  - issueType : BALLPARK - Alpha\n" +
                "    tshirtCustomFieldId: Alpha_TestTshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + alphaIssueType + "\n"
                );

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt","Review_Tshirt"));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).priorityOrder(1l),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(3)
                    .tshirt("Dev_Tshirt", "L")
                    .priorityOrder(1l),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL").priorityOrder(1l).originalEstimateInHours(7)
        );

        List<FromJiraDataRow> rows = subject.generate(ZoneId.systemDefault(), DEFAULT_PROJECT).fromJiraDs.rows;
        assertEquals((Double)3.0, rows.get(0).wrongWorklog);
    }
    
    @Test
    public void ifDemandAndFeatureHasWorklog_SubtaskShouldHaveSumOfWrongWorklog() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n" +

                "  - issueType : BALLPARK - Alpha\n" +
                "    tshirtCustomFieldId: Alpha_TestTshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + alphaIssueType + "\n"
                );

        tshirtSizeInfo.setIds(asList("Dev_Tshirt","Alpha_TestTshirt","Review_Tshirt"));

        issues( 
            demand().id(2).key("PROJ-2").summary("Smry 2").originalEstimateInHours(1).priorityOrder(1l).timeSpentInHours(4),

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(3)
                    .tshirt("Dev_Tshirt", "L")
                    .priorityOrder(1l),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL").priorityOrder(1l).originalEstimateInHours(7)
        );

        List<FromJiraDataRow> rows = subject.generate(ZoneId.systemDefault(), DEFAULT_PROJECT).fromJiraDs.rows;
        assertEquals((Double)7.0, rows.get(0).wrongWorklog);
    }

    @Test
    public void worklogDataShouldBeSet() {
        configureBallparkMappings(
                taskIssueType + " : \n" +
                "  - issueType : BALLPARK - Development\n" +
                "    tshirtCustomFieldId: Dev_Tshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + devIssueType + "\n" +

                "  - issueType : BALLPARK - Alpha\n" +
                "    tshirtCustomFieldId: Alpha_TestTshirt\n" +
                "    jiraIssueTypes:\n" +
                "      - " + alphaIssueType + "\n"
                );
        issues(
                task().id(3).key("PROJ-3"),
                subtask().id(4).key("PROJ-4").parent("PROJ-3").worklog("john", "2018-11-27", 300).issueType(devIssueType)
            );

        List<FromJiraDataRow> rows = subject.generate(ZoneId.systemDefault(), DEFAULT_PROJECT).fromJiraDs.rows;
        Worklog worklog = rows.get(0).worklogs.get(0);

        assertEquals("john", worklog.author);
        assertEquals(300, worklog.timeSpentSeconds);
        assertEquals("2018-11-27T02:00:00Z", worklog.started.toInstant().toString());
    }

    private List<FromJiraDataRow> sortJiraDataByIssuesKeys(List<FromJiraDataRow> actual) {
        Collections.sort(actual, new Comparator<FromJiraDataRow>() {
            @Override
            public int compare(FromJiraDataRow o1, FromJiraDataRow o2) {
                return (o1.demandNum + o1.taskNum + o1.subtaskNum).compareTo(o2.demandNum + o2.taskNum + o2.subtaskNum);
            }
        });
        return actual;
    }

    private void assertFollowupsForIssuesEqualsOrdered(String expectedFollowupList) {
        List<FromJiraDataRow> actual = subject.generate(ZoneId.systemDefault(), DEFAULT_PROJECT).fromJiraDs.rows;
        assertEquals(expectedFollowupList, fromJiraRowstoString(actual, "\n\n"));
    }

    private void assertFollowupsForIssuesEquals(String expectedFollowupList) {
        List<FromJiraDataRow> actual = sortJiraDataByIssuesKeys(subject.generate(ZoneId.systemDefault(), DEFAULT_PROJECT).fromJiraDs.rows);

        assertEquals(
            expectedFollowupList,
            fromJiraRowstoString(actual, "\n\n"));
    }
}
