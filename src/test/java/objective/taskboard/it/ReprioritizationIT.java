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
package objective.taskboard.it;

import org.junit.Test;

public class ReprioritizationIT extends AuthenticatedIntegrationTest {
    @Test
    public void whenIssueIsDragged_AfterReloadItShouldKeepOrder() {
        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.errorToast().close();
        
        LaneFragment operational = mainPage.lane("Operational");
        operational.boardStep("To Do").assertIssueList(
                "TASKB-625",
                "TASKB-627",
                "TASKB-643",
                "TASKB-644",
                "TASKB-659",
                "TASKB-661",
                "TASKB-663",
                "TASKB-664",
                "TASKB-680",
                "TASKB-681",
                "TASKB-682",
                "TASKB-683",
                "TASKB-684",
                "TASKB-686"
                );

        mainPage.issue("TASKB-643").dragOver("TASKB-627");

        operational.boardStep("To Do").assertIssueList(
                "TASKB-625",
                "TASKB-643",
                "TASKB-627",
                "TASKB-644",
                "TASKB-659",
                "TASKB-661",
                "TASKB-663",
                "TASKB-664",
                "TASKB-680",
                "TASKB-681",
                "TASKB-682",
                "TASKB-683",
                "TASKB-684",
                "TASKB-686"
                );
    }

    @Test
    public void whenIssueIsPriorityOrderIsChanged_ShouldShowNotificationInAnotherBrowserAndUpdateTheOrder() {
        createAndSwitchToNewTab();
        
        MainPage secondTabPage = MainPage.to(webDriver);
        secondTabPage.waitUserLabelToBe("foo");
        secondTabPage.errorToast().close();
        
        LaneFragment operationalInSecondTab = secondTabPage.lane("Operational");
        secondTabPage.issue("TASKB-643").dragOver("TASKB-627");
        operationalInSecondTab.boardStep("To Do").assertIssueList(
                "TASKB-625",
                "TASKB-643",
                "TASKB-627",
                "TASKB-644",
                "TASKB-659",
                "TASKB-661",
                "TASKB-663",
                "TASKB-664",
                "TASKB-680",
                "TASKB-681",
                "TASKB-682",
                "TASKB-683",
                "TASKB-684",
                "TASKB-686"
                );
        
        switchToFirstTab();
        
        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.refreshToast().assertVisible();
        LaneFragment operational = mainPage.lane("Operational");
        operational.boardStep("To Do").assertIssueList(
                "TASKB-625",
                "TASKB-643",
                "TASKB-627",
                "TASKB-644",
                "TASKB-659",
                "TASKB-661",
                "TASKB-663",
                "TASKB-664",
                "TASKB-680",
                "TASKB-681",
                "TASKB-682",
                "TASKB-683",
                "TASKB-684",
                "TASKB-686"
                );
        
        mainPage.refreshToast().dismiss();
        // makes sure the model is correctly updated
        mainPage.typeSearch("TASKB-625");
        mainPage.clearSearch();
        operational.boardStep("To Do").assertIssueList(
                "TASKB-625",
                "TASKB-643",
                "TASKB-627",
                "TASKB-644",
                "TASKB-659",
                "TASKB-661",
                "TASKB-663",
                "TASKB-664",
                "TASKB-680",
                "TASKB-681",
                "TASKB-682",
                "TASKB-683",
                "TASKB-684",
                "TASKB-686"
                );        
    }
}
