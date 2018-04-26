package objective.taskboard.it;

import org.junit.Test;

public class CardTeamsIT extends AuthenticatedIntegrationTest {
    @Test
    public void whenIssueIsOpenWithouTeam_ShouldHaveDefaultTeam(){
        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.errorToast().close();
        TestIssue issue = mainPage.issue("TASKB-625");
        issue
            .click()
            .issueDetails()
            .assertIsDefaultTeam("TASKBOARD 1");
    }

    @Test
    public void whenAddTeam_ShouldUpdateIssueImmediatlyWithNewTeam(){
        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.errorToast().close();
        TestIssue issue = mainPage.issue("TASKB-647");
        issue
            .click()
            .issueDetails()
            .addTeam("FFC")
            .assertTeams("TASKBOARD 2", "FFC");
    }

    @Test
    public void whenTeamIsReplaced_ShouldUpdateIssueImmediatlyWithNewTeam(){
        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.errorToast().close();
        TestIssue issue = mainPage.issue("TASKB-625");
        issue
            .click()
            .issueDetails()
            .replaceTeam("TASKBOARD 1", "FFC")
            .assertTeams("FFC");
    }

    @Test
    public void whenRemovingTeam_ShouldUpdateIssueImmediatlyWithNewTeam(){
        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.errorToast().close();
        TestIssue issue = mainPage.issue("TASKB-647");
        issue
            .click()
            .issueDetails()
            .addTeam("FFC")
            .removeTeam("TASKBOARD 2")
            .assertTeams("FFC");
    }
}
