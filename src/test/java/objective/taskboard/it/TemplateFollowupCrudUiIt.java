package objective.taskboard.it;

import org.junit.Test;

public class TemplateFollowupCrudUiIt extends AuthenticatedIntegrationTest {
    @Test
    public void whenCreateANewFollowupTemplate_VerifyIfItHasNameProjectsAndFile() {
        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.assertFollowupButtonIsNotVisible()
            .openTemplateFollowUpDialog()
            .tryToCreateATemplateWithoutName()
            .tryToCreateATemplateWithoutSelectARole()
            .tryToCreateATemplateWithoutSelectAFile()
            .createATemplate("Template Success Test", 0, 1);
        mainPage.reload()
            .assertFollowupButtonIsVisible();
    }
}
