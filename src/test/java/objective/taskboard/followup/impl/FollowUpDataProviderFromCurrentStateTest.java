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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import objective.taskboard.followup.FromJiraDataRow;

public class FollowUpDataProviderFromCurrentStateTest extends AbstractFollowUpDataProviderTest {

    @Test
    public void demandWithoutChildFeatures_shouldCreateASingleBallpark() {
        issues( 
            demand().id(1).key("PROJ-1").summary("Smry 1").originalEstimateInHours(1).timeSpentInHours(10)
        );

        assertFollowupsForIssuesEquals(
            " planningType           : Ballpark\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 1\n" + 
            " demandNum              : PROJ-1\n" + 
            " demandSummary          : Smry 1\n" + 
            " demandDescription      : M | 00001 - Smry 1\n" + 
            " taskType               : BALLPARK - Demand\n" + 
            " taskStatus             : Open\n" + 
            " taskId                 : 0\n" + 
            " taskNum                : PROJ-1\n" + 
            " taskSummary            : Dummy Feature\n" + 
            " taskDescription        : 00000 - Smry 1\n" + 
            " taskFullDescription    : BALLPARK - Demand | M | 00000 - Smry 1\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : BALLPARK - Demand\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 0\n" + 
            " subtaskNum             : PROJ-0\n" + 
            " subtaskSummary         : Smry 1\n" + 
            " subtaskDescription     : M | 00000 - Smry 1\n" + 
            " subtaskFullDescription : BALLPARK - Demand | M | 00000 - Smry 1\n" + 
            " tshirtSize             : M\n" + 
            " worklog                : 0.0\n" + 
            " wrongWorklog           : 10.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : DEMAND BALLPARK"
        );
    }

    @Test
    public void demandWithRelease_shouldCreateABallparkWithReleaseInfo() {
        issues( 
            demand().id(1).key("PROJ-1").summary("Smry 1").originalEstimateInHours(1).timeSpentInHours(10).release("Release 42")
        );

        assertFollowupsForIssuesEquals(
            " planningType           : Ballpark\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 1\n" + 
            " demandNum              : PROJ-1\n" + 
            " demandSummary          : Smry 1\n" + 
            " demandDescription      : M | 00001 - Smry 1\n" + 
            " taskType               : BALLPARK - Demand\n" + 
            " taskStatus             : Open\n" + 
            " taskId                 : 0\n" + 
            " taskNum                : PROJ-1\n" + 
            " taskSummary            : Dummy Feature\n" + 
            " taskDescription        : 00000 - Smry 1\n" + 
            " taskFullDescription    : BALLPARK - Demand | M | 00000 - Smry 1\n" + 
            " taskRelease            : Release 42\n" + 
            " subtaskType            : BALLPARK - Demand\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 0\n" + 
            " subtaskNum             : PROJ-0\n" + 
            " subtaskSummary         : Smry 1\n" + 
            " subtaskDescription     : M | 00000 - Smry 1\n" + 
            " subtaskFullDescription : BALLPARK - Demand | M | 00000 - Smry 1\n" + 
            " tshirtSize             : M\n" + 
            " worklog                : 0.0\n" + 
            " wrongWorklog           : 10.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : DEMAND BALLPARK"
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
            " planningType           : Ballpark\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 1\n" + 
            " demandNum              : PROJ-1\n" + 
            " demandSummary          : Smry 1\n" + 
            " demandDescription      : M | 00001 - Smry 1\n" + 
            " taskType               : BALLPARK - Demand\n" + 
            " taskStatus             : Open\n" + 
            " taskId                 : 0\n" + 
            " taskNum                : PROJ-1\n" + 
            " taskSummary            : Dummy Feature\n" + 
            " taskDescription        : 00000 - Smry 1\n" + 
            " taskFullDescription    : BALLPARK - Demand | M | 00000 - Smry 1\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : BALLPARK - Demand\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 0\n" + 
            " subtaskNum             : PROJ-0\n" + 
            " subtaskSummary         : Smry 1\n" + 
            " subtaskDescription     : M | 00000 - Smry 1\n" + 
            " subtaskFullDescription : BALLPARK - Demand | M | 00000 - Smry 1\n" + 
            " tshirtSize             : M\n" + 
            " worklog                : 0.0\n" + 
            " wrongWorklog           : 10.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : DEMAND BALLPARK"+

            "\n\n"+

            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : Dev\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 4\n" + 
            " subtaskNum             : PROJ-4\n" + 
            " subtaskSummary         : Smry 4\n" + 
            " subtaskDescription     : XL | 00004 - Smry 4\n" + 
            " subtaskFullDescription : To Do > Dev | XL | 00004 - Smry 4\n" + 
            " tshirtSize             : XL\n" + 
            " worklog                : 5.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : SUBTASK PLAN");
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
                task().id(3).key("PROJ-3").summary("Smry 3").originalEstimateInHours(2).parent("PROJ-2").priorityOrder(1l)
                    .tshirt("Dev_Tshirt",      "S")
                    .tshirt("FrontDev_Tshirt", "S"),
                subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL").priorityOrder(1l)
        );

        assertFollowupsForIssuesEquals(
            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : Dev\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 4\n" + 
            " subtaskNum             : PROJ-4\n" + 
            " subtaskSummary         : Smry 4\n" + 
            " subtaskDescription     : XL | 00004 - Smry 4\n" + 
            " subtaskFullDescription : To Do > Dev | XL | 00004 - Smry 4\n" + 
            " tshirtSize             : XL\n" + 
            " worklog                : 5.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : SUBTASK PLAN");
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
            " planningType           : Ballpark\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : BALLPARK - Development\n" + 
            " subtaskStatus          : Open\n" + 
            " subtaskId              : 0\n" + 
            " subtaskNum             : PROJ-0\n" + 
            " subtaskSummary         : BALLPARK - Development\n" + 
            " subtaskDescription     : 00000 - Smry 3\n" + 
            " subtaskFullDescription : BALLPARK - Development | 00000 - Smry 3\n" + 
            " tshirtSize             : L\n" + 
            " worklog                : 0.0\n" + 
            " wrongWorklog           : 1.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 2.0\n" + 
            " queryType              : FEATURE BALLPARK"+

            "\n\n"+

            " planningType           : Ballpark\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : BALLPARK - Alpha\n" + 
            " subtaskStatus          : Open\n" + 
            " subtaskId              : 0\n" + 
            " subtaskNum             : PROJ-0\n" + 
            " subtaskSummary         : BALLPARK - Alpha\n" + 
            " subtaskDescription     : 00000 - Smry 3\n" + 
            " subtaskFullDescription : BALLPARK - Alpha | 00000 - Smry 3\n" + 
            " tshirtSize             : S\n" + 

            " worklog                : 0.0\n" + 
            " wrongWorklog           : 1.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 2.0\n" + 
            " queryType              : FEATURE BALLPARK");
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

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                    .tshirt("Dev_Tshirt", "L")
                    .tshirt("Alpha_TestTshirt", "S")
                    .priorityOrder(1l),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL").priorityOrder(1l)
        );

        assertFollowupsForIssuesEquals(
            " planningType           : Ballpark\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : BALLPARK - Alpha\n" + 
            " subtaskStatus          : Open\n" + 
            " subtaskId              : 0\n" + 
            " subtaskNum             : PROJ-0\n" + 
            " subtaskSummary         : BALLPARK - Alpha\n" + 
            " subtaskDescription     : 00000 - Smry 3\n" + 
            " subtaskFullDescription : BALLPARK - Alpha | 00000 - Smry 3\n" + 
            " tshirtSize             : S\n" + 
            " worklog                : 0.0\n" + 
            " wrongWorklog           : 1.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 2.0\n" + 
            " queryType              : FEATURE BALLPARK" +

            "\n\n"+

            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : Dev\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 4\n" + 
            " subtaskNum             : PROJ-4\n" + 
            " subtaskSummary         : Smry 4\n" + 
            " subtaskDescription     : XL | 00004 - Smry 4\n" + 
            " subtaskFullDescription : To Do > Dev | XL | 00004 - Smry 4\n" + 
            " tshirtSize             : XL\n" + 
            " worklog                : 5.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : SUBTASK PLAN");
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

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                    .tshirt("Dev_Tshirt", "L")
                    .tshirt("Alpha_TestTshirt", "S")
                    .priorityOrder(1l),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL").priorityOrder(1l),

            subtask().id(5).key("PROJ-5").summary("Smry 5").timeSpentInHours(15).parent("PROJ-3").issueType(alphaIssueType).tshirtSize("L").priorityOrder(1l)
        );

        assertFollowupsForIssuesEquals(
            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : Dev\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 4\n" + 
            " subtaskNum             : PROJ-4\n" + 
            " subtaskSummary         : Smry 4\n" + 
            " subtaskDescription     : XL | 00004 - Smry 4\n" + 
            " subtaskFullDescription : To Do > Dev | XL | 00004 - Smry 4\n" + 
            " tshirtSize             : XL\n" + 
            " worklog                : 5.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : SUBTASK PLAN" +

            "\n\n"+

            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : Alpha\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 5\n" + 
            " subtaskNum             : PROJ-5\n" + 
            " subtaskSummary         : Smry 5\n" + 
            " subtaskDescription     : L | 00005 - Smry 5\n" + 
            " subtaskFullDescription : To Do > Alpha | L | 00005 - Smry 5\n" + 
            " tshirtSize             : L\n" + 
            " worklog                : 15.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : SUBTASK PLAN");
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
            " planningType           : Ballpark\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : release 66\n" + 
            " subtaskType            : BALLPARK - Development\n" + 
            " subtaskStatus          : Open\n" + 
            " subtaskId              : 0\n" + 
            " subtaskNum             : PROJ-0\n" + 
            " subtaskSummary         : BALLPARK - Development\n" + 
            " subtaskDescription     : 00000 - Smry 3\n" + 
            " subtaskFullDescription : BALLPARK - Development | 00000 - Smry 3\n" + 
            " tshirtSize             : L\n" + 
            " worklog                : 0.0\n" + 
            " wrongWorklog           : 1.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 2.0\n" + 
            " queryType              : FEATURE BALLPARK"
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

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                    .tshirt("Dev_Tshirt", "L").release("release 66"),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL")
        );

        assertFollowupsForIssuesEquals(
            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : release 66\n" + 
            " subtaskType            : Dev\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 4\n" + 
            " subtaskNum             : PROJ-4\n" + 
            " subtaskSummary         : Smry 4\n" + 
            " subtaskDescription     : XL | 00004 - Smry 4\n" + 
            " subtaskFullDescription : To Do > Dev | XL | 00004 - Smry 4\n" + 
            " tshirtSize             : XL\n" + 
            " worklog                : 5.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : SUBTASK PLAN"
            );
    }
    
    @Test
    public void featureWithSubtaskWithoutTShirt_ShouldUseParentEstimativeInTaskBallpark() {
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

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                    .tshirt("Dev_Tshirt", "L").release("release 66"),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType)
        );

        assertFollowupsForIssuesEquals(
            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : release 66\n" + 
            " subtaskType            : Dev\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 4\n" + 
            " subtaskNum             : PROJ-4\n" + 
            " subtaskSummary         : Smry 4\n" + 
            " subtaskDescription     : 00004 - Smry 4\n" + 
            " subtaskFullDescription : To Do > Dev | 00004 - Smry 4\n" + 
            " tshirtSize             : \n" + 
            " worklog                : 5.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 2.0\n" + 
            " queryType              : SUBTASK PLAN"
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
            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : \n" + 
            " demandStatus           : \n" + 
            " demandId               : \n" + 
            " demandNum              : \n" + 
            " demandSummary          : \n" + 
            " demandDescription      : \n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : Dev\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 4\n" + 
            " subtaskNum             : PROJ-4\n" + 
            " subtaskSummary         : Smry 4\n" + 
            " subtaskDescription     : XL | 00004 - Smry 4\n" + 
            " subtaskFullDescription : To Do > Dev | XL | 00004 - Smry 4\n" + 
            " tshirtSize             : XL\n" + 
            " worklog                : 5.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 0.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : SUBTASK PLAN");
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

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                    .tshirt("Dev_Tshirt", "L").priorityOrder(1l),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL")
                .issueStatus(statusCancelled).priorityOrder(1l)
        );

        assertFollowupsForIssuesEquals(
            " planningType           : Ballpark\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : BALLPARK - Development\n" + 
            " subtaskStatus          : Open\n" + 
            " subtaskId              : 0\n" + 
            " subtaskNum             : PROJ-0\n" + 
            " subtaskSummary         : BALLPARK - Development\n" + 
            " subtaskDescription     : 00000 - Smry 3\n" + 
            " subtaskFullDescription : BALLPARK - Development | 00000 - Smry 3\n" + 
            " tshirtSize             : L\n" + 
            " worklog                : 0.0\n" + 
            " wrongWorklog           : 1.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 2.0\n" + 
            " queryType              : FEATURE BALLPARK"+

            "\n\n"+

            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : Dev\n" + 
            " subtaskStatus          : Cancelled\n" + 
            " subtaskId              : 4\n" + 
            " subtaskNum             : PROJ-4\n" + 
            " subtaskSummary         : Smry 4\n" + 
            " subtaskDescription     : XL | 00004 - Smry 4\n" + 
            " subtaskFullDescription : Cancelled > Dev | XL | 00004 - Smry 4\n" + 
            " tshirtSize             : XL\n" + 
            " worklog                : 5.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : SUBTASK PLAN");
    }

    @Test
    public void withoutBallparkConfiguration_ShouldFail() {
        issues( 
                task().id(3).key("PROJ-3").summary("Smry 3").originalEstimateInHours(2),
                subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL")
        );

        try {
            subject.getJiraData(defaultProjects());
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
            " planningType           : Ballpark\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : release 88\n" + 
            " subtaskType            : BALLPARK - Development\n" + 
            " subtaskStatus          : Open\n" + 
            " subtaskId              : 0\n" + 
            " subtaskNum             : PROJ-0\n" + 
            " subtaskSummary         : BALLPARK - Development\n" + 
            " subtaskDescription     : 00000 - Smry 3\n" + 
            " subtaskFullDescription : BALLPARK - Development | 00000 - Smry 3\n" + 
            " tshirtSize             : L\n" + 
            " worklog                : 0.0\n" + 
            " wrongWorklog           : 1.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 2.0\n" + 
            " queryType              : FEATURE BALLPARK"
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
            " planningType           : Ballpark\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : release 66\n" + 
            " subtaskType            : BALLPARK - Development\n" + 
            " subtaskStatus          : Open\n" + 
            " subtaskId              : 0\n" + 
            " subtaskNum             : PROJ-0\n" + 
            " subtaskSummary         : BALLPARK - Development\n" + 
            " subtaskDescription     : 00000 - Smry 3\n" + 
            " subtaskFullDescription : BALLPARK - Development | 00000 - Smry 3\n" + 
            " tshirtSize             : L\n" + 
            " worklog                : 0.0\n" + 
            " wrongWorklog           : 1.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 2.0\n" + 
            " queryType              : FEATURE BALLPARK"
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

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
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
            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : Sub Task Release #3\n" + 
            " subtaskType            : Dev\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 4\n" + 
            " subtaskNum             : PROJ-4\n" + 
            " subtaskSummary         : Smry 4\n" + 
            " subtaskDescription     : XL | 00004 - Smry 4\n" + 
            " subtaskFullDescription : To Do > Dev | XL | 00004 - Smry 4\n" + 
            " tshirtSize             : XL\n" + 
            " worklog                : 5.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : SUBTASK PLAN" +

            "\n\n"+

            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : Sub Task Release #4\n" + 
            " subtaskType            : Dev\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 5\n" + 
            " subtaskNum             : PROJ-5\n" + 
            " subtaskSummary         : Smry 5\n" + 
            " subtaskDescription     : XL | 00005 - Smry 5\n" + 
            " subtaskFullDescription : To Do > Dev | XL | 00005 - Smry 5\n" + 
            " tshirtSize             : XL\n" + 
            " worklog                : 5.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : SUBTASK PLAN"             
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

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                    .tshirt("Dev_Tshirt", "L")
                    .tshirt("Alpha_TestTshirt", "S"),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL")
        );

        assertFollowupsForIssuesEquals(
            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : Demand Release #1\n" + 
            " subtaskType            : Dev\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 4\n" + 
            " subtaskNum             : PROJ-4\n" + 
            " subtaskSummary         : Smry 4\n" + 
            " subtaskDescription     : XL | 00004 - Smry 4\n" + 
            " subtaskFullDescription : To Do > Dev | XL | 00004 - Smry 4\n" + 
            " tshirtSize             : XL\n" + 
            " worklog                : 5.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : SUBTASK PLAN"
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
                " planningType           : Ballpark\n" +
                " project                : A Project\n" +
                " demandType             : Demand\n" +
                " demandStatus           : Done\n" +
                " demandId               : 3\n" +
                " demandNum              : PROJ-3\n" +
                " demandSummary          : Smry 3\n" +
                " demandDescription      : M | 00003 - Smry 3\n" +
                " taskType               : BALLPARK - Demand\n" +
                " taskStatus             : Open\n" +
                " taskId                 : 0\n" +
                " taskNum                : PROJ-3\n" +
                " taskSummary            : Dummy Feature\n" +
                " taskDescription        : 00000 - Smry 3\n" +
                " taskFullDescription    : BALLPARK - Demand | M | 00000 - Smry 3\n" +
                " taskRelease            : No release set\n" +
                " subtaskType            : BALLPARK - Demand\n" +
                " subtaskStatus          : Done\n" +
                " subtaskId              : 0\n" +
                " subtaskNum             : PROJ-0\n" +
                " subtaskSummary         : Smry 3\n" +
                " subtaskDescription     : M | 00000 - Smry 3\n" +
                " subtaskFullDescription : BALLPARK - Demand | M | 00000 - Smry 3\n" +
                " tshirtSize             : M\n" +
                " worklog                : 0.0\n" +
                " wrongWorklog           : 0.0\n" +
                " demandBallpark         : 0.0\n" +
                " taskBallpark           : 0.0\n" +
                " queryType              : DEMAND BALLPARK" +

                "\n\n" +

                " planningType           : Ballpark\n" +
                " project                : A Project\n" +
                " demandType             : Demand\n" +
                " demandStatus           : To Do\n" +
                " demandId               : 4\n" +
                " demandNum              : PROJ-4\n" +
                " demandSummary          : Smry 4\n" +
                " demandDescription      : M | 00004 - Smry 4\n" +
                " taskType               : BALLPARK - Demand\n" +
                " taskStatus             : Open\n" +
                " taskId                 : 0\n" +
                " taskNum                : PROJ-4\n" +
                " taskSummary            : Dummy Feature\n" +
                " taskDescription        : 00000 - Smry 4\n" +
                " taskFullDescription    : BALLPARK - Demand | M | 00000 - Smry 4\n" +
                " taskRelease            : No release set\n" +
                " subtaskType            : BALLPARK - Demand\n" +
                " subtaskStatus          : To Do\n" +
                " subtaskId              : 0\n" +
                " subtaskNum             : PROJ-0\n" +
                " subtaskSummary         : Smry 4\n" +
                " subtaskDescription     : M | 00000 - Smry 4\n" +
                " subtaskFullDescription : BALLPARK - Demand | M | 00000 - Smry 4\n" +
                " tshirtSize             : M\n" +
                " worklog                : 0.0\n" +
                " wrongWorklog           : 0.0\n" +
                " demandBallpark         : 0.0\n" +
                " taskBallpark           : 0.0\n" +
                " queryType              : DEMAND BALLPARK" +

                "\n\n" +

                " planningType           : Ballpark\n" +
                " project                : A Project\n" +
                " demandType             : Demand\n" +
                " demandStatus           : To Do\n" +
                " demandId               : 2\n" +
                " demandNum              : PROJ-2\n" +
                " demandSummary          : Smry 2\n" +
                " demandDescription      : M | 00002 - Smry 2\n" +
                " taskType               : BALLPARK - Demand\n" +
                " taskStatus             : Open\n" +
                " taskId                 : 0\n" +
                " taskNum                : PROJ-2\n" +
                " taskSummary            : Dummy Feature\n" +
                " taskDescription        : 00000 - Smry 2\n" +
                " taskFullDescription    : BALLPARK - Demand | M | 00000 - Smry 2\n" +
                " taskRelease            : No release set\n" +
                " subtaskType            : BALLPARK - Demand\n" +
                " subtaskStatus          : To Do\n" +
                " subtaskId              : 0\n" +
                " subtaskNum             : PROJ-0\n" +
                " subtaskSummary         : Smry 2\n" +
                " subtaskDescription     : M | 00000 - Smry 2\n" +
                " subtaskFullDescription : BALLPARK - Demand | M | 00000 - Smry 2\n" +
                " tshirtSize             : M\n" +
                " worklog                : 0.0\n" +
                " wrongWorklog           : 0.0\n" +
                " demandBallpark         : 0.0\n" +
                " taskBallpark           : 0.0\n" +
                " queryType              : DEMAND BALLPARK" +

                "\n\n" +

                " planningType           : Ballpark\n" +
                " project                : A Project\n" +
                " demandType             : Demand\n" +
                " demandStatus           : Open\n" +
                " demandId               : 1\n" +
                " demandNum              : PROJ-1\n" +
                " demandSummary          : Smry 1\n" +
                " demandDescription      : M | 00001 - Smry 1\n" +
                " taskType               : BALLPARK - Demand\n" +
                " taskStatus             : Open\n" +
                " taskId                 : 0\n" +
                " taskNum                : PROJ-1\n" +
                " taskSummary            : Dummy Feature\n" +
                " taskDescription        : 00000 - Smry 1\n" +
                " taskFullDescription    : BALLPARK - Demand | M | 00000 - Smry 1\n" +
                " taskRelease            : No release set\n" +
                " subtaskType            : BALLPARK - Demand\n" +
                " subtaskStatus          : Open\n" +
                " subtaskId              : 0\n" +
                " subtaskNum             : PROJ-0\n" +
                " subtaskSummary         : Smry 1\n" +
                " subtaskDescription     : M | 00000 - Smry 1\n" +
                " subtaskFullDescription : BALLPARK - Demand | M | 00000 - Smry 1\n" +
                " tshirtSize             : M\n" +
                " worklog                : 0.0\n" +
                " wrongWorklog           : 0.0\n" +
                " demandBallpark         : 0.0\n" +
                " taskBallpark           : 0.0\n" +
                " queryType              : DEMAND BALLPARK"
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

        subject.getJiraData(defaultProjects());
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

        subject.getJiraData(defaultProjects());
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
                " planningType           : Ballpark\n" + 
                " project                : A Project\n" + 
                " demandType             : \n" + 
                " demandStatus           : \n" + 
                " demandId               : \n" + 
                " demandNum              : \n" + 
                " demandSummary          : \n" + 
                " demandDescription      : \n" + 
                " taskType               : Task\n" + 
                " taskStatus             : To Do\n" + 
                " taskId                 : 3\n" + 
                " taskNum                : PROJ-3\n" + 
                " taskSummary            : Smry 3\n" + 
                " taskDescription        : 00003 - Smry 3\n" + 
                " taskFullDescription    : Task | 00003 - Smry 3\n" + 
                " taskRelease            : No release set\n" + 
                " subtaskType            : BALLPARK - Development\n" + 
                " subtaskStatus          : Open\n" + 
                " subtaskId              : 0\n" + 
                " subtaskNum             : PROJ-0\n" + 
                " subtaskSummary         : BALLPARK - Development\n" + 
                " subtaskDescription     : 00000 - Smry 3\n" + 
                " subtaskFullDescription : BALLPARK - Development | 00000 - Smry 3\n" + 
                " tshirtSize             : L\n" + 
                " worklog                : 0.0\n" + 
                " wrongWorklog           : 1.0\n" + 
                " demandBallpark         : 0.0\n" + 
                " taskBallpark           : 2.0\n" + 
                " queryType              : FEATURE BALLPARK");
    }

    @Test
    public void issuesOfDifferentProjects_ShouldOnlyIncludeOfSelectectedProjects() {
        issues( 
                demand().id(1).key("PROJ-1").summary("Smry 1").originalEstimateInHours(1).timeSpentInHours(10),
                demand().id(2).project("ANOTHER").key("ANOTHER-1").summary("Smry 2").originalEstimateInHours(1).timeSpentInHours(10)
            );

        assertFollowupsForIssuesEquals(
            " planningType           : Ballpark\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 1\n" + 
            " demandNum              : PROJ-1\n" +         
            " demandSummary          : Smry 1\n" + 
            " demandDescription      : M | 00001 - Smry 1\n" + 
            " taskType               : BALLPARK - Demand\n" + 
            " taskStatus             : Open\n" + 
            " taskId                 : 0\n" + 
            " taskNum                : PROJ-1\n" + 
            " taskSummary            : Dummy Feature\n" + 
            " taskDescription        : 00000 - Smry 1\n" + 
            " taskFullDescription    : BALLPARK - Demand | M | 00000 - Smry 1\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : BALLPARK - Demand\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 0\n" + 
            " subtaskNum             : PROJ-0\n" + 
            " subtaskSummary         : Smry 1\n" + 
            " subtaskDescription     : M | 00000 - Smry 1\n" + 
            " subtaskFullDescription : BALLPARK - Demand | M | 00000 - Smry 1\n" + 
            " tshirtSize             : M\n" + 
            " worklog                : 0.0\n" + 
            " wrongWorklog           : 10.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : DEMAND BALLPARK"
        );
    }

    @Test
    public void ifSubtaskHasOriginalEstimate_taskBallparkHasToShowThatValue() {
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

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                    .tshirt("Dev_Tshirt", "L")
                    .tshirt("Alpha_TestTshirt", "S")
                    .priorityOrder(1l),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).priorityOrder(1l).originalEstimateInHours(7),
            subtask().id(5).key("PROJ-5").summary("Smry 5").timeSpentInHours(15).parent("PROJ-3").issueType(alphaIssueType).tshirtSize("L").priorityOrder(1l)
        );

        assertFollowupsForIssuesEquals(
            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : Dev\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 4\n" + 
            " subtaskNum             : PROJ-4\n" + 
            " subtaskSummary         : Smry 4\n" + 
            " subtaskDescription     : 00004 - Smry 4\n" + 
            " subtaskFullDescription : To Do > Dev | 00004 - Smry 4\n" + 
            " tshirtSize             : \n" + 
            " worklog                : 5.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 7.0\n" + 
            " queryType              : SUBTASK PLAN" +

            "\n\n"+

            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : Alpha\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 5\n" + 
            " subtaskNum             : PROJ-5\n" + 
            " subtaskSummary         : Smry 5\n" + 
            " subtaskDescription     : L | 00005 - Smry 5\n" + 
            " subtaskFullDescription : To Do > Alpha | L | 00005 - Smry 5\n" + 
            " tshirtSize             : L\n" + 
            " worklog                : 15.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : SUBTASK PLAN");
    }
    
    @Test
    public void ifSubtaskHasOriginalEstimateAndTshirt_shouldNotFillTaskballPark() {
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

            task()  .id(3).key("PROJ-3").parent("PROJ-2").summary("Smry 3").originalEstimateInHours(2).timeSpentInHours(1)
                    .tshirt("Dev_Tshirt", "L")
                    .priorityOrder(1l),

            subtask().id(4).key("PROJ-4").summary("Smry 4").timeSpentInHours(5).parent("PROJ-3").issueType(devIssueType).tshirtSize("XL").priorityOrder(1l).originalEstimateInHours(7)
        );

        assertFollowupsForIssuesEquals(
            " planningType           : Plan\n" + 
            " project                : A Project\n" + 
            " demandType             : Demand\n" + 
            " demandStatus           : To Do\n" + 
            " demandId               : 2\n" + 
            " demandNum              : PROJ-2\n" + 
            " demandSummary          : Smry 2\n" + 
            " demandDescription      : 00002 - Smry 2\n" + 
            " taskType               : Task\n" + 
            " taskStatus             : To Do\n" + 
            " taskId                 : 3\n" + 
            " taskNum                : PROJ-3\n" + 
            " taskSummary            : Smry 3\n" + 
            " taskDescription        : 00003 - Smry 3\n" + 
            " taskFullDescription    : Task | 00003 - Smry 3\n" + 
            " taskRelease            : No release set\n" + 
            " subtaskType            : Dev\n" + 
            " subtaskStatus          : To Do\n" + 
            " subtaskId              : 4\n" + 
            " subtaskNum             : PROJ-4\n" + 
            " subtaskSummary         : Smry 4\n" + 
            " subtaskDescription     : XL | 00004 - Smry 4\n" + 
            " subtaskFullDescription : To Do > Dev | XL | 00004 - Smry 4\n" + 
            " tshirtSize             : XL\n" + 
            " worklog                : 5.0\n" + 
            " wrongWorklog           : 0.0\n" + 
            " demandBallpark         : 1.0\n" + 
            " taskBallpark           : 0.0\n" + 
            " queryType              : SUBTASK PLAN");
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
        List<FromJiraDataRow> actual = subject.getJiraData(defaultProjects()).fromJiraDs.rows;
        assertEquals(expectedFollowupList, StringUtils.join(actual, "\n\n"));
    }

    private void assertFollowupsForIssuesEquals(String expectedFollowupList) {
        List<FromJiraDataRow> actual = sortJiraDataByIssuesKeys(subject.getJiraData(defaultProjects()).fromJiraDs.rows);

        assertEquals(
            expectedFollowupList,
            StringUtils.join(actual, "\n\n"));
    }

}
