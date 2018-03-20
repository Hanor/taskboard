package objective.taskboard.it;

import org.junit.Test;

public class InvalidTeamIT extends AuthenticatedIntegrationTest {

    private static final String ERROR_MESSAGE_INVALID_TEAM = "Some users are assigned to tasks that are not in their teams.";

    @Test
    public void givenIssueWithInvalidTeam_whenChangeFilters_thenIssueShouldBeFiltered() {
        MainPage mainPage = MainPage.produce(webDriver);
        mainPage
            .errorToast()
            .assertErrorMessage(ERROR_MESSAGE_INVALID_TEAM);
        
        mainPage
            .errorToast()
            .clickButtonWithText("Close and show cards with problems");
        
        mainPage
            .assertVisibleIssues("TASKB-20", "TASKB-626", "TASKB-637", "TASKB-639", "TASKB-685", "TASKB-624", "TASKB-625", "TASKB-627");
    }
}