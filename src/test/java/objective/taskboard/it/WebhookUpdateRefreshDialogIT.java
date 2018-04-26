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
package objective.taskboard.it;

import org.junit.Test;

import objective.taskboard.RequestBuilder;
import objective.taskboard.utils.IOUtilities;

public class WebhookUpdateRefreshDialogIT extends AuthenticatedIntegrationTest {
    private static final String STANDARD_COLOR = "rgb(238, 238, 238)";
    private static final String FIXED_DATE_COLOR = "rgb(254, 229, 188)";

    @Test
    public void whenUpdateHappensViaWebHookAndUpdatedIssueIsOpen_ShouldWarnUser() {
        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.errorToast().close();
        IssueDetails issueDetails = mainPage
            .issue("TASKB-625")
            .click()
            .issueDetails();
        
        emulateUpdateIssue("TASKB-625", "{\"assignee\":{\"name\":\"foo\"}},\"properties\":[]");
        
        issueDetails
            .assertRefreshWarnIsOpen()
            .clickOnRefreshWarning()
            .assertAssignees("foo","gtakeuchi");

        emulateDeleteIssue("TASKB-625");

        issueDetails
            .assertDeleteWarnIsOpen()
            .clickOnDeleteWarning()
            .assertIsClosed();
    }

    @Test
    public void givenAnIssueNotVisible_whenUpdateHappensViaWebHook_RefreshToastShouldNotShowUP() {
        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.typeSearch("TASKB-625");

        BoardStepFragment stepToDo = mainPage.lane("Operational")
                .boardStep("To Do");
        stepToDo.assertIssueList("TASKB-625");

        mainPage.typeSearch("TASKB-61")
            .assertVisibleIssues("TASKB-611", "TASKB-612", "TASKB-613", "TASKB-610", "TASKB-614");

        emulateUpdateIssue("TASKB-625", "{\"status\":{\"id\": \"10652\",\"name\": \"Doing\"}}");

        mainPage.refreshToast().assertNotVisible();
        mainPage.typeSearch("TASKB-625");
        stepToDo.assertIssueList();
        mainPage.lane("Operational")
            .boardStep("Doing")
            .assertIssueList("TASKB-625");
    }

    @Test
    public void givenVisibleIssuesWithChildren_whenParentGoToDeferred_thenAllChildrenShouldDisappear() {
        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.issue("TASKB-606")
            .enableHierarchicalFilter();
        mainPage.assertVisibleIssues("TASKB-606", "TASKB-186", "TASKB-235", "TASKB-601", "TASKB-572");
        mainPage.issue("TASKB-606")
            .enableHierarchicalFilter();

        emulateUpdateIssue("TASKB-606", "{\"status\":{\"id\": \"10655\",\"name\": \"Deferred\"}}");

        mainPage.errorToast().close();
        mainPage.refreshToast().assertNotVisible();
        mainPage.typeSearch("TASKB-606").assertVisibleIssues()
            .typeSearch("TASKB-186").assertVisibleIssues()
            .typeSearch("TASKB-235").assertVisibleIssues()
            .typeSearch("TASKB-601").assertVisibleIssues()
            .typeSearch("TASKB-572").assertVisibleIssues()
            .refreshToast().assertNotVisible();
    }

    @Test
    public void givenIssuesWithChildren_whenParentChangeTheClassOfService_thenAllChildrenShouldUpdate() {
        MainPage mainPage = MainPage.produce(webDriver);

        mainPage.issue("TASKB-606").assertCardColor(STANDARD_COLOR).click().issueDetails()
            .assertClassOfService("Standard")
            .closeDialog();
        mainPage.issue("TASKB-186").assertCardColor(STANDARD_COLOR).click().issueDetails()
            .assertClassOfService("Standard")
            .closeDialog();
        mainPage.issue("TASKB-235").assertCardColor(STANDARD_COLOR).click().issueDetails()
            .assertClassOfService("Standard")
            .closeDialog();
        mainPage.issue("TASKB-601").assertCardColor(STANDARD_COLOR).click().issueDetails()
            .assertClassOfService("Standard")
            .closeDialog();
        mainPage.issue("TASKB-572").assertCardColor(STANDARD_COLOR).click().issueDetails()
            .assertClassOfService("Standard")
            .closeDialog();

        emulateUpdateIssue("TASKB-606", "{\"customfield_11440\":{\"id\": \"12607\",\"value\": \"Fixed Date\"}}");

        String[] updatedIssues = {"TASKB-606", "TASKB-186", "TASKB-235", "TASKB-601", "TASKB-572"};
        mainPage.assertUpdatedIssues(updatedIssues);
        mainPage.refreshToast().assertVisible().showOnlyUpdated();
        mainPage.assertVisibleIssues(updatedIssues);

        mainPage.issue("TASKB-606").assertCardColor(FIXED_DATE_COLOR).click().issueDetails()
            .assertClassOfService("Fixed Date")
            .closeDialog();
        mainPage.issue("TASKB-186").assertCardColor(FIXED_DATE_COLOR).click().issueDetails()
            .assertClassOfService("Fixed Date")
            .closeDialog();
        mainPage.issue("TASKB-235").assertCardColor(FIXED_DATE_COLOR).click().issueDetails()
            .assertClassOfService("Fixed Date")
            .closeDialog();
        mainPage.issue("TASKB-601").assertCardColor(FIXED_DATE_COLOR).click().issueDetails()
            .assertClassOfService("Fixed Date")
            .closeDialog();
        mainPage.issue("TASKB-572").assertCardColor(FIXED_DATE_COLOR).click().issueDetails()
            .assertClassOfService("Fixed Date");
    }

    @Test
    public void whenDeleteIssueHappensViaWebhook_thenIssueShouldDisappear() {
        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.typeSearch("TASKB-625")
            .assertVisibleIssues("TASKB-625");

        emulateDeleteIssue("TASKB-625");

        mainPage.refreshToast().assertNotVisible();
        mainPage.assertVisibleIssues()
            .refreshToast().assertNotVisible();
    }

    private static void emulateUpdateIssue(String issueKey, String fieldsJson) {
        RequestBuilder
            .url("http://localhost:4567/rest/api/latest/issue/" + issueKey)
            .body("{\"fields\":" + fieldsJson + "}")
            .put();

        String body = IOUtilities.resourceToString("webhook/" + issueKey + "_updatePayload.json");
        RequestBuilder
            .url(getSiteBase()+"/webhook/TASKB")
            .header("Content-Type", "application/json")
            .body(body)
            .post();
    }

    private static void emulateDeleteIssue(String issueKey) {
        RequestBuilder
            .url("http://localhost:4567/rest/api/latest/issue/" + issueKey)
            .delete();

        String body = IOUtilities.resourceToString("webhook/" + issueKey + "_deletePayload.json");
        RequestBuilder
            .url(getSiteBase()+"/webhook/TASKB")
            .header("Content-Type", "application/json")
            .body(body)
            .post();
    }

    public static void main(String[] args) {
        emulateUpdateIssue("TASKB-625", "{\"assignee\":{\"name\":\"foo\"}},\"properties\":[]");
    }
}
