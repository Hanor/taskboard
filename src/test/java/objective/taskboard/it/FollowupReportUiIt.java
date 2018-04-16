package objective.taskboard.it;

import static java.util.Arrays.asList;

import org.junit.Test;

import objective.taskboard.it.FollowupReportType.FollowupReportTypeData;

public class FollowupReportUiIt extends AuthenticatedIntegrationTest {
    @Test
    public void whenCreateANewFollowupReportType_VerifyIfItHasNameFileAndRoles() {
        MainPage mainPage = MainPage.produce(webDriver);
        FollowupReport reportWindow = mainPage.assertFollowupButtonIsVisible()
            .openFollowUpReport();

        reportWindow
            .assertReportTypes(asList())
            .clickAddLink()
            .tryCreateReportType(new FollowupReportTypeData())
                .assertErrorMessage()
            .tryCreateReportType(new FollowupReportTypeData().withName())
                .assertErrorMessage()
            .tryCreateReportType(new FollowupReportTypeData().withName().withFile())
                .assertErrorMessage()
            .tryCreateReportType(new FollowupReportTypeData().withName().withRoles())
                .assertErrorMessage()
            .tryCreateReportType(new FollowupReportTypeData().withFile())
                .assertErrorMessage()
            .tryCreateReportType(new FollowupReportTypeData().withFile().withRoles())
                .assertErrorMessage()
            .tryCreateReportType(new FollowupReportTypeData().withRoles())
                .assertErrorMessage()
            .tryCreateReportType(new FollowupReportTypeData().withName().withFile().withRoles())
                .waitClose();

        reportWindow
            .assertReportTypes(asList("Report Type Test"));
    }

    @Test
    public void whenEditAFollowupReportType_VerifyIfItWasEdited() {
        MainPage mainPage = MainPage.produce(webDriver);
        FollowupReport reportWindow = mainPage.assertFollowupButtonIsVisible()
            .openFollowUpReport();

        reportWindow
            .clickAddLink()
            .tryCreateReportType(new FollowupReportTypeData().withName().withFile().withRoles())
            .waitClose();

        reportWindow
            .assertReportTypes(asList("Report Type Test"))
            .clickEditReportType()
            .close()
            .waitClose();

        reportWindow
            .assertReportTypes(asList("Report Type Test"))
            .clickEditReportType()
            .setName("Report Type Test edited")
            .close()
                .clickOnCancelDiscardMessage()
            .close()
                .clickOnConfirmDiscardMessage()
            .waitClose();

        reportWindow
            .assertReportTypes(asList("Report Type Test"))
            .clickEditReportType()
            .editReportType();

        reportWindow
            .assertReportTypes(asList("Report Type Test edited"));
    }

    @Test
    public void whenDeleteAFollowupReportType_VerifyIfItWasDeleted() {
        MainPage mainPage = MainPage.produce(webDriver);
        FollowupReport reportWindow = mainPage.assertFollowupButtonIsVisible()
            .openFollowUpReport();

        reportWindow
            .clickAddLink()
            .tryCreateReportType(new FollowupReportTypeData().withName().withFile().withRoles())
            .waitClose();

        reportWindow
            .assertReportTypes(asList("Report Type Test"))
            .clickDeleteReportType()
                .clickOnCancelDelete()
            .clickDeleteReportType()
                .clickOnConfirmDelete();

        reportWindow
            .assertReportTypes(asList());
    }

}
