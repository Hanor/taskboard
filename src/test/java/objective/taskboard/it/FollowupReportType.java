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

import static org.openqa.selenium.support.PageFactory.initElements;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;

public class FollowupReportType extends AbstractUiFragment {
    private WebElement saveButton;
    private WebElement nameInput;
    private WebElement fileInput;
    private WebElement dropFileMessage;
    private List<WebElement> rolesCheckbox;
    
    private final String ERROR_MESSAGE = "Make sure the name is not empty, the report type file has been uploaded, " +
            "and at least one role has been selected.";

    @FindBy(id="followupReportType")
    private WebElement followupReportType;

    public static FollowupReportType produce(WebDriver webDriver) {
        return initElements(webDriver, FollowupReportType.class);
    }

    public static FollowupReportType open(WebDriver webDriver) {
        return produce(webDriver).open();
    }

    private FollowupReportType open() {
        saveButton = followupReportType.findElement(By.id("save"));
        nameInput = followupReportType.findElement(By.cssSelector("#templateNameInputl input"));
        rolesCheckbox = followupReportType.findElements(By.cssSelector("paper-checkbox"));
        waitVisibilityOfElements(saveButton, nameInput);
        waitVisibilityOfElementList(rolesCheckbox);

        dropFileMessage = followupReportType.findElement(By.cssSelector("drag-and-drop-file #dropFileMessage"));
        fileInput = followupReportType.findElement(By.cssSelector("drag-and-drop-file #inputFile")); // isInvisible
        return this;
    }
    
    public FollowupReportType close() {
        WebElement close = followupReportType.findElement(By.cssSelector(".modal__close"));
        waitForClick(close);
        return this;
    }

    public FollowupReportType(WebDriver driver) {
        super(driver);
    }

    public FollowupReportType clickOnSave() {
        waitForClick(saveButton);
        return this;
    }

    public FollowupReportType setName(String name) {
        nameInput.sendKeys(Keys.CONTROL,"a");
        nameInput.sendKeys(Keys.DELETE);
        nameInput.sendKeys(name);
        return this;
    }

    public FollowupReportType setFile() {
        File file = null;
        try {
            file = new File(FollowupReportType.class.getResource("/objective/taskboard/followup/OkFollowupTemplate.xlsm").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        sendKeysToFileInput(fileInput, file.toString());
        return this;
    }

    public FollowupReportType clearFile() {
        if (!dropFileMessage.isDisplayed()) {
            WebElement clearFileButton = followupReportType.findElement(By.id("clearFileButton"));
            waitForClick(clearFileButton);
        }
        return this;
    }

    public FollowupReportType setRoles() {
        waitForClick(rolesCheckbox.get(0));
        return this;
    }

    public FollowupReportType clearRoles() {
        for (WebElement role : rolesCheckbox)
            if (role.getAttribute("checked") != null && role.getAttribute("checked").equals("true"))
                waitForClick(role);
        return this;
    }

    public FollowupReportType tryCreateReportType(FollowupReportTypeData reportType) {
        setName("");
        clearFile();
        clearRoles();

        if (reportType.withName)
            setName("Report Type Test");

        if (reportType.withFile)
            setFile();

        if (reportType.withRoles)
            setRoles();

        clickOnSave();
        return this;
    }
    
    public void waitClose() {
        waitInvisibilityOfElement(saveButton);
    }

    public FollowupReportType editReportType() {
        setName("Report Type Test edited");
        clickOnSave();
        clickOnConfirmUpdate();
        waitClose();
        return this;
    }

    private void clickOnConfirmUpdate() {
        WebElement confirm = followupReportType.findElement(By.cssSelector("#confirmationModal #confirm"));
        waitForClick(confirm);
    }

    public FollowupReportType clickOnCancelDiscardMessage() {
        WebElement cancel = followupReportType.findElement(By.cssSelector("#modal #confirmationModal #cancel"));
        waitForClick(cancel);
        return this;
    }

    public FollowupReportType clickOnConfirmDiscardMessage() {
        WebElement confirm = followupReportType.findElement(By.cssSelector("#modal #confirmationModal #confirm"));
        waitForClick(confirm);
        return this;
    }

    public FollowupReportType assertErrorMessage() {
        waitUntil(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver input) {
                WebElement errorMessage = followupReportType.findElement(By.cssSelector("#alertModal .text"));
                waitVisibilityOfElement(errorMessage);
                return ERROR_MESSAGE.equals(errorMessage.getText()); 
            }
        });
        closeAlertDialog();
        return this;
    }

    private void closeAlertDialog() {
        WebElement closeAlertDialog = followupReportType.findElement(By.cssSelector("#alertModal #ok"));
        waitForClick(closeAlertDialog);
    }

    public static class FollowupReportTypeData {
        boolean withName = false;
        boolean withFile = false;
        boolean withRoles = false;

        FollowupReportTypeData() { }

        public FollowupReportTypeData withName() {
            withName = true;
            return this;
        }

        public FollowupReportTypeData withFile() {
            withFile = true;
            return this;
        }

        public FollowupReportTypeData withRoles() {
            withRoles = true;
            return this;
        }
    }
}
