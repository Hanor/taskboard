package objective.taskboard.it;

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

import static org.openqa.selenium.support.PageFactory.initElements;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;

public class FollowupReport extends AbstractUiFragment {
    private WebElement generateButton;
    private WebElement dateDropdown;
    private WebElement clearDateButton;
    private WebElement addLink;

    public FollowupReport(WebDriver driver) {
        super(driver);
    }

    @FindBy(css=".followup-button")
    private WebElement followupButton;
    
    @FindBy(id="followupReport")
    private WebElement followupReport;
    
    public static FollowupReport open(WebDriver webDriver) {
        return initElements(webDriver, FollowupReport.class).open();
    }
    
    private FollowupReport open() {
        waitForClick(followupButton);
        waitVisibilityOfElement(followupReport);
        generateButton = followupReport.findElement(By.id("generate"));
        dateDropdown = followupReport.findElement(By.name("date"));
        clearDateButton = followupReport.findElement(By.cssSelector(".clear-button"));
        addLink = followupReport.findElement(By.className("add-link"));
        return this;
    }
    
    public FollowupReport selectAProject(int projectIndex) {
        List<WebElement> projectsCheckbox = followupReport.findElements(By.cssSelector("paper-checkbox"));
        WebElement projectToSelect = projectsCheckbox.get(projectIndex);
        waitForClick(projectToSelect);
        waitUntilPaperCheckboxSelectionStateToBe(projectToSelect, true);
        return this;
    }

    public FollowupReport selectADate(String date) {
        waitForClick(dateDropdown);
        WebElement dateElement = getPaperDropdownMenuItemByValue(dateDropdown, date);
        waitForClick(dateElement);
        waitPaperDropdownMenuItemIsSelected(dateElement, true);
        return this;
    }

    public FollowupReport clickClearDate() {
        waitForClick(clearDateButton);
        return this;
    }

    public FollowupReportType clickAddLink() {
        waitForClick(addLink);
        return FollowupReportType.open(webDriver);
    }

    public FollowupReport assertGenerateButtonIsDisabled() {
        waitUntil(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver input) {
                return generateButton.getAttribute("disabled") != null; 
            }
        });
        return this;
    }
    
    public FollowupReport assertGenerateButtonIsEnabled() {
        waitUntil(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver input) {
                return generateButton.getAttribute("disabled") == null; 
            }
        });
        return this;
    }

    public FollowupReport assertDateIsToday() {
        waitAttributeValueInElement(dateDropdown, "value", "Today");
        return this;
    }

    public FollowupReport assertDateDropdownIsInvisible() {
        waitInvisibilityOfElement(dateDropdown);
        return this;
    }

    public FollowupReport close() {
        WebElement close = followupReport.findElement(By.cssSelector(".modal__close"));
        waitForClick(close);
        return this;
    }

    public FollowupReport assertReportTypes(List<String> reportTypesExpected) {
        List<WebElement> reportTypes = followupReport.findElements(By.cssSelector("paper-radio-button"));
        if (reportTypes.size() > 0)
            waitVisibilityOfElementList(reportTypes);
        assertEquals(reportTypesExpected.size(), reportTypes.size());
        List<String> reportTypesActual = reportTypes.stream()
            .map(r -> r.getText())
            .collect(Collectors.toList());
        assertThat(reportTypesActual, equalTo(reportTypesExpected));
        return this;
    }

    public FollowupReportType clickEditReportType() {
        WebElement editButton = followupReport.findElement(By.id("editButton"));
        waitForClick(editButton);
        return FollowupReportType.open(webDriver);
    }

    public FollowupReport clickDeleteReportType() {
        WebElement deleteButton = followupReport.findElement(By.id("deleteButton"));
        waitForClick(deleteButton);
        return this;
    }

    public FollowupReport clickOnCancelDelete() {
        WebElement cancel = followupReport.findElement(By.cssSelector("#confirmationModal #cancel"));
        waitForClick(cancel);
        return this;
    }

    public FollowupReport clickOnConfirmDelete() {
        WebElement confirm = followupReport.findElement(By.cssSelector("#confirmationModal #confirm"));
        waitForClick(confirm);
        return this;
    }
}
