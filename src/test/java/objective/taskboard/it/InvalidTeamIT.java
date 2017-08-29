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

public class InvalidTeamIT extends AuthenticatedIntegrationTest {

    private static final String ISSUE_KEY_INVALID_TEAM = "TASKB-20";
    private static final String ISSUE_KEY_VALID_TEAM = "TASKB-611";
    private static final String ERROR_MESSAGE_INVALID_TEAM = "Assignee must be on team of project: TASKB-20: diego.prandini; Total: 1";
    private static final String FILTER_ISSUE_TYPE = "Issue Type";
    private static final String FILTER_PROJECT = "Project";
    private static final String FILTER_TEAM = "Team";

    @Test
    public void givenIssueWithInvalidTeam_whenChangeFilters_thenIssueShouldBeFiltered() {
        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.typeSearch(ISSUE_KEY_INVALID_TEAM)
            .assertVisibleIssues(ISSUE_KEY_INVALID_TEAM)
            .errorToast()
            .assertErrorMessage(ERROR_MESSAGE_INVALID_TEAM);

        MenuFilters menuFilters = mainPage.openMenuFilters()
            .openAspectsFilter()
            .clickCheckAllFilter(FILTER_ISSUE_TYPE);
        mainPage.assertVisibleIssues()
            .errorToast()
            .assertErrorToastIsInvisible();

        menuFilters.clickCheckAllFilter(FILTER_ISSUE_TYPE);
        mainPage.assertVisibleIssues(ISSUE_KEY_INVALID_TEAM)
            .errorToast()
            .assertErrorMessage(ERROR_MESSAGE_INVALID_TEAM);

        menuFilters.clickCheckAllFilter(FILTER_PROJECT);
        mainPage.assertVisibleIssues()
            .errorToast()
            .assertErrorToastIsInvisible();

        menuFilters.clickCheckAllFilter(FILTER_PROJECT);
        mainPage.assertVisibleIssues(ISSUE_KEY_INVALID_TEAM)
            .errorToast()
            .assertErrorMessage(ERROR_MESSAGE_INVALID_TEAM);

        menuFilters.clickCheckAllFilter(FILTER_TEAM);
        mainPage.assertVisibleIssues()
            .errorToast()
            .assertErrorToastIsInvisible();

        menuFilters.clickCheckAllFilter(FILTER_TEAM);
        mainPage.assertVisibleIssues(ISSUE_KEY_INVALID_TEAM)
            .errorToast()
            .assertErrorMessage(ERROR_MESSAGE_INVALID_TEAM);

        menuFilters.closeMenuFilters();
        mainPage.clearSearch()
            .issue(ISSUE_KEY_VALID_TEAM)
            .click()
            .issueDetails()
            .assertInvalidTeamWarnIsInvisible()
            .closeDialog();
        mainPage.issue(ISSUE_KEY_INVALID_TEAM)
            .click()
            .issueDetails()
            .assertInvalidTeamWarnIsVisible();
    }

}
