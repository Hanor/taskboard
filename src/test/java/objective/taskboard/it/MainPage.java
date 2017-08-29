
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

import static org.apache.commons.lang3.StringUtils.join;
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.support.PageFactory.initElements;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElementValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class MainPage extends AbstractUiFragment {
    @FindBy(css=".nameButton.user-account")
    private WebElement userLabelButton;

    @FindBy(id = "searchIssues")
    private WebElement searchIssuesInput;

    @FindBy(id = "searchRelease")
    private WebElement searchReleaseDropdown;

    @FindBy(css = ".menuLink")
    private WebElement menuFiltersButton;
    
    public MainPage(WebDriver webDriver) {
        super(webDriver);
    }
    
    public static MainPage produce(WebDriver webDriver) {
        return initElements(webDriver, MainPage.class);
    }
    
    public static MainPage to(WebDriver webDriver) {
        webDriver.get(AbstractUIIntegrationTest.getSiteBase()+"/");
        return initElements(webDriver, MainPage.class);
    }
    
    public MainPage reload() {
        webDriver.navigate().refresh();
        waitUserLabelToBe("foo");
        return this;
    }

    public void waitUserLabelToBe(String expected) {
        waitTextInElement(userLabelButton, expected);
    }
    
    public MainPage typeSearch(String searchValue) {
        searchIssuesInput.sendKeys(Keys.CONTROL,"a");
        searchIssuesInput.sendKeys(Keys.DELETE);
        searchIssuesInput.sendKeys(searchValue);
        waitUntil(textToBePresentInElementValue(searchIssuesInput, searchValue));
        return this;
    }
    
    public MainPage clearSearch() {
        searchIssuesInput.sendKeys(Keys.CONTROL,"a");
        searchIssuesInput.sendKeys(Keys.DELETE);
        waitUntil(textToBePresentInElementValue(searchIssuesInput, ""));
        return this;
    }

    public MenuFilters openMenuFilters() {
        waitVisibilityOfElement(menuFiltersButton);
        menuFiltersButton.click();
        return initElements(webDriver, MenuFilters.class);
    }

    public MainPage filterByRelease(String release) {
        String releaseNotNull = release == null ? "" : release;

        waitVisibilityOfElement(searchReleaseDropdown);
        searchReleaseDropdown.click();

        WebElement releaseElement = searchReleaseDropdown.findElements(By.tagName("paper-item")).stream()
            .filter(paperItem -> releaseNotNull.equals(paperItem.getText()))
            .findFirst().orElse(null);

        if (releaseElement == null)
            throw new IllegalArgumentException("Element \"" + releaseNotNull + "\" of Release filter not found");

        waitVisibilityOfElement(releaseElement);
        releaseElement.click();
        return this;
    }

    public MainPage assertLabelRelease(String expected) {
        waitTextInElement(searchReleaseDropdown, expected);
        return this;
    }

    public MainPage assertVisibleIssues(String ... expectedIssueKeyList) {
        waitUntil(ExpectedConditions.numberOfElementsToBe(By.cssSelector("paper-material.issue"), expectedIssueKeyList.length));
        List<WebElement> findElements = webDriver.findElements(By.cssSelector("paper-material.issue"));
        ArrayList<String> actualIssueKeyList = new ArrayList<String>(); 
        for (WebElement webElement : findElements) 
            actualIssueKeyList.add( webElement.findElement(By.cssSelector(".key.issue-item")).getText().trim());
        Arrays.sort(expectedIssueKeyList);
        Collections.sort(actualIssueKeyList);
        
        assertEquals(join(expectedIssueKeyList,"\n"), join(actualIssueKeyList,"\n"));
        return this;
    }

    public TestIssue issue(String issueKey) {
        return TestIssue.forKey(webDriver, issueKey);
    }

    public RefreshToast refreshToast() {
        return initElements(webDriver, RefreshToast.class);
    }

    public ErrorToast errorToast() {
        return initElements(webDriver, ErrorToast.class);
    }

    public FollowupDialog openFollowUp() {
        return FollowupDialog.open(webDriver);
    }
    
    public TemplateFollowupDialog openTemplateFollowUpDialog() {
        return TemplateFollowupDialog.open(webDriver);
    }

    public LaneFragment lane(String laneName) {
        return LaneFragment.laneName(webDriver, laneName);
    }

    public IssueDetails issueDetails() {
        return new IssueDetails(webDriver);
    }

    public void assertOkIcon() {
        waitVisibilityOfElement(webDriver.findElement(By.className("issue-state-ready")));
    }
    
    public SizingImportUi openSizingImport() {
        return SizingImportUi.open(webDriver);
    }

    public void assertStatusIconIsInitialisationError() {
        waitVisibilityOfElement(webDriver.findElement(By.className("issue-state-initialisationError")));
    }
}
