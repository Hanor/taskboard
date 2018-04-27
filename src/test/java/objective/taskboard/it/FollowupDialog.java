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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;

public class FollowupDialog extends AbstractUiFragment {
    private WebElement generateButton;
    private WebElement dateDropdown;
    private WebElement clearDateButton;
    private WebElement noMatchingTemplateWarning;
    private WebElement setTemplateLink;

    @FindBy(css=".followup-button")
    private WebElement followupButton;
    
    @FindBy(id="followupdialog")
    private WebElement dialog;
    
    public static FollowupDialog open(WebDriver webDriver) {
        return initElements(webDriver, FollowupDialog.class).open();
    }
    
    private FollowupDialog open() {
        waitForClick(followupButton);
        waitVisibilityOfElement(dialog);
        generateButton = dialog.findElement(By.id("generate"));
        dateDropdown = dialog.findElement(By.name("date"));
        clearDateButton = dialog.findElement(By.cssSelector(".clear-button"));
        noMatchingTemplateWarning = dialog.findElement(By.id("noTemplateWarning"));
        setTemplateLink = dialog.findElement(By.className("set-template-link"));
        return this;
    }
    
    public FollowupDialog selectAProject(int projectIndex) {
        List<WebElement> projectsCheckbox = dialog.findElements(By.cssSelector("paper-checkbox"));
        WebElement projectToSelect = projectsCheckbox.get(projectIndex);
        waitForClick(projectToSelect);
        waitUntilPaperCheckboxSelectionStateToBe(projectToSelect, true);
        return this;
    }

    public FollowupDialog selectADate(String date) {
        selectPaperDropdownItem(dateDropdown, date);
        return this;
    }

    public FollowupDialog clickClearDate() {
        waitForClick(clearDateButton);
        return this;
    }

    public TemplateFollowupDialog clickSetTemplateLink() {
        waitForClick(setTemplateLink);
        return TemplateFollowupDialog.produce(webDriver);
    }

    public FollowupDialog assertGenerateButtonIsDisabled() {
        waitUntil(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver input) {
                return generateButton.getAttribute("disabled") != null; 
            }
        });
        return this;
    }
    
    public FollowupDialog assertGenerateButtonIsEnabled() {
        waitUntil(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver input) {
                return generateButton.getAttribute("disabled") == null; 
            }
        });
        return this;
    }

    public FollowupDialog assertDateIsToday() {
        waitAttributeValueInElement(dateDropdown, "value", "Today");
        return this;
    }

    public FollowupDialog assertDateDropdownIsInvisible() {
        waitInvisibilityOfElement(dateDropdown);
        return this;
    }

    public FollowupDialog assertNoMatchingTemplateWarningIsInvisible() {
        waitInvisibilityOfElement(noMatchingTemplateWarning);
        return this;
    }

    public FollowupDialog assertNoMatchingTemplateWarningIsVisible() {
        waitVisibilityOfElement(noMatchingTemplateWarning);
        return this;
    }

    public FollowupDialog close() {
        WebElement close = dialog.findElement(By.cssSelector(".modal__close"));
        waitForClick(close);
        return this;
    }

    public FollowupDialog(WebDriver driver) {
        super(driver);
    }


}
