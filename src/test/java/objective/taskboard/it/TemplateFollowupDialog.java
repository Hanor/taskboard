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
import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;

public class TemplateFollowupDialog extends AbstractUiFragment {
    private WebElement newTemplateItem;
    private WebElement createButton;
    private WebElement templateNameInput;
    private WebElement templateFileLabel;
    private WebElement templateFileInput;
    private List<WebElement> rolesCheckbox;
    
    private final String DEFAUT_ERROR_MESSAGE = "Make sure the name is not empty, at least one role has been selected, " +
            "and that the template file has been uploaded.";
    
    @FindBy(css=".template-followup-button")
    private WebElement teplateFollowupButton;
    
    @FindBy(id="followupCrudModal")
    private WebElement dialog;

    public static TemplateFollowupDialog produce(WebDriver webDriver) {
        return initElements(webDriver, TemplateFollowupDialog.class);
    }

    public static TemplateFollowupDialog open(WebDriver webDriver) {
        return produce(webDriver).open();
    }
    
    private TemplateFollowupDialog open() {
        waitForClick(teplateFollowupButton);

        newTemplateItem = dialog.findElement(By.id("newTemplateItem"));
        createButton = dialog.findElement(By.id("createTemplate"));
        templateNameInput = dialog.findElement(By.cssSelector("#templateNameInputl input"));
        templateFileLabel = dialog.findElement(By.cssSelector("label[for=inputTemplate]"));
        rolesCheckbox = dialog.findElements(By.cssSelector("paper-checkbox"));
        waitVisibilityOfElements(newTemplateItem, createButton, templateNameInput, templateFileLabel);
        waitVisibilityOfElementList(rolesCheckbox);

        templateFileInput = dialog.findElement(By.id("inputTemplate")); // isInvisible
        return this;
    }
    
    public TemplateFollowupDialog close() {
        WebElement close = dialog.findElement(By.cssSelector(".modal__close"));
        waitForClick(close);
        return this;
    }

    public TemplateFollowupDialog(WebDriver driver) {
        super(driver);
    }
    
    public TemplateFollowupDialog tryToCreateATemplateWithoutName() {
        waitForClick(newTemplateItem);
        waitForClick(createButton);
        assertErrorMessage(DEFAUT_ERROR_MESSAGE);
        closeAlertDialog();
        return this;
    }

    public TemplateFollowupDialog tryToCreateATemplateWithoutSelectARole() {
        waitForClick(newTemplateItem);
        templateNameInput.sendKeys("Template test");
        waitForClick(createButton);
        assertErrorMessage(DEFAUT_ERROR_MESSAGE);
        closeAlertDialog();
        return this;
    }

    public TemplateFollowupDialog tryToCreateATemplateWithoutSelectAFile() {
        waitForClick(newTemplateItem);
        templateNameInput.sendKeys("Template test");
        waitForClick(rolesCheckbox.get(0));
        waitForClick(createButton);
        assertErrorMessage(DEFAUT_ERROR_MESSAGE);
        closeAlertDialog();
        return this;
    }
    
    public TemplateFollowupDialog createATemplate(String templateName, Integer... rolesIndex) {
        File file = null;
        
        try {
            file = new File(TemplateFollowupDialog.class.getResource("/objective/taskboard/followup/OkFollowupTemplate.xlsm").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        
        close();
        open();
        waitForClick(newTemplateItem);
        templateNameInput.sendKeys(templateName);
        for (Integer roleIndex : rolesIndex)
            waitForClick(rolesCheckbox.get(roleIndex));

        sendKeysToFileInput(templateFileInput, file.toString());
        waitForClick(createButton);
        
        waitUntil(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver input) {
                List<WebElement> templates = dialog
                        .findElements(By.cssSelector(".template-item"));
                
                Optional<WebElement> templateCreated = templates
                        .stream()
                        .filter(t -> templateName.equals(t.getText()))
                        .findFirst();
                
                return templateCreated.isPresent(); 
            }
        });
        return this;
    }

    private void assertErrorMessage(String message) {
        waitUntil(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver input) {
                WebElement errorMessage = dialog.findElement(By.cssSelector("#alertModalTemplate .text"));
                waitVisibilityOfElement(errorMessage);
                return message.equals(errorMessage.getText()); 
            }
        });
    }
    
    private void closeAlertDialog() {
        WebElement closeAlertDialog = dialog.findElement(By.cssSelector("#alertModalTemplate #ok"));
        waitForClick(closeAlertDialog);
    }

}
