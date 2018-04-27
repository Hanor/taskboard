package objective.taskboard.it;

import org.junit.Before;
import org.junit.Test;

public class ProjectConfigurationUiIT extends AuthenticatedIntegrationTest {

    private MainPage mainPage;
    
    @Before
    public void setup() {
        mainPage = MainPage.produce(webDriver);
    }
    
    @Test
    public void shouldValidate() {
        openProjectConfigurationDialog("TASKB")
            .setStartDate("99999")
            .setDeliveryDate("99/20/2018")
            .setRisk("-50")
            .update()
            .assertStartDateError("Please enter a valid date")
            .assertDeliveryDateError("Please enter a valid date")
            .assertRiskError("Please enter a positive value or zero")

            .setStartDate("01/20/2018")
            .setDeliveryDate("01/15/2018")
            .setRisk("0")
            .update()
            .assertGlobalError("End Date should be greater than Start Date");
    }
    
    @Test
    public void shouldPersist() {
        openProjectConfigurationDialog("TASKB")
            .setStartDate("01/25/2018")
            .setDeliveryDate("10/20/2018")
            .setRisk("60.99")
            .setBaselineDate("03/30/2018")
            .update()
            .assertSuccessAlertIsOpen()
            .closeSuccessAlert();
        
        openProjectConfigurationDialog("TASKB")
            .assertStartDate("01/25/2018")
            .assertDeliveryDate("10/20/2018")
            .assertRisk("60.99")
            .assertBaselineDate("03/30/2018");
    }

    private ProjectConfigurationDialog openProjectConfigurationDialog(String projectKey) {
        return mainPage.openMenuFilters()
            .openProjectsConfiguration()
            .openProjectConfigurationModal(projectKey);
    }
}
