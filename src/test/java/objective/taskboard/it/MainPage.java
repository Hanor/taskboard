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

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.By.tagName;
import static org.openqa.selenium.support.PageFactory.initElements;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElementValue;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;

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
        waitUserLabelToBe("Foo");
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
        waitForClick(menuFiltersButton);
        return initElements(webDriver, MenuFilters.class);
    }

    public MainPage waitReleaseFilterContains(String release) {
        String releaseNotNull = defaultIfNull(release, "");

        waitVisibilityOfElement(searchReleaseDropdown);
        waitUntilElementExists(cssSelector("#searchRelease paper-item"));
        waitPaperDropdownMenuContains(searchReleaseDropdown, releaseNotNull);

        return this;
    }

    public MainPage filterByRelease(String release) {
        String releaseNotNull = release == null ? "" : release;

        waitForClick(searchReleaseDropdown);
        waitUntilElementExists(cssSelector("#searchRelease paper-item"));

        WebElement releaseElement = searchReleaseDropdown.findElements(tagName("paper-item")).stream()
            .filter(paperItem -> releaseNotNull.equals(paperItem.getText()))
            .findFirst().orElse(null);

        if (releaseElement == null)
            throw new IllegalArgumentException("Element \"" + releaseNotNull + "\" of Release filter not found");

        waitForClick(releaseElement);
        return this;
    }

    public MainPage assertLabelRelease(String expected) {
        waitPaperDropdownMenuSelectedValueToBe(searchReleaseDropdown, expected);
        return this;
    }

    public MainPage assertUpdatedIssues(String ... expectedIssueKeyList) {
        assertIssues(cssSelector("paper-material.issue.issue-UPDATED"), expectedIssueKeyList);
        return this;
    }

    public MainPage assertVisibleIssues(String... expectedIssueKeyList) {
        assertIssues(cssSelector("paper-material.issue"), expectedIssueKeyList);
        return this;
    }

    public TestIssue issue(String issueKey) {
        return new TestIssue(webDriver, issueKey);
    }

    public RefreshToast refreshToast() {
        return initElements(webDriver, RefreshToast.class);
    }

    public ErrorToast errorToast() {
        return initElements(webDriver, ErrorToast.class);
    }

    public IssueErrorToast issueErrorToast() {
        return initElements(webDriver, IssueErrorToast.class);
    }

    public FollowupReport openFollowUpReport() {
        return FollowupReport.open(webDriver);
    }

    public LaneFragment lane(String laneName) {
        return LaneFragment.laneName(webDriver, laneName);
    }

    public IssueDetails issueDetails() {
        return new IssueDetails(webDriver);
    }

    public void assertOkIcon() {
        waitVisibilityOfElement(webDriver.findElement(className("issue-state-ready")));
    }

    public SizingImportUi openSizingImport() {
        return SizingImportUi.open(webDriver);
    }

    public void assertStatusIconIsInitialisationError() {
        waitVisibilityOfElement(webDriver.findElement(className("issue-state-initialisationError")));
    }

    private void assertIssues(By by, String... expectedIssueKeyList) {
        waitUntil(new ExpectedCondition<Boolean>() {
            private String[] actualIssueKeyList;
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    actualIssueKeyList = driver.findElements(by).stream()
                        .map(i -> i.findElement(cssSelector(".key.issue-item")).getText().trim())
                        .toArray(String[]::new);
                    return Arrays.equals(expectedIssueKeyList, actualIssueKeyList);
                } catch (StaleElementReferenceException e) {
                    return null;
                }
            }
            @Override
            public String toString() {
                return String.format("issue key list to be \"%s\". Current issue key list: \"%s\"",
                        StringUtils.join(expectedIssueKeyList, ","),
                        StringUtils.join(actualIssueKeyList, ","));
            }
        });
    }

    public MainPage assertFollowupButtonIsVisible() {
        return assertButtonExistsAndVisible("followup-button");
    }

    public MainPage assertFollowupButtonIsNotVisible() {
        waitUntilElementNotExists(className("followup-button"));
        return this;
    }

    public MainPage assertDashboardButtonIsVisible() {
        return assertButtonExistsAndVisible("dashboard-button");
    }

    public MainPage assertDashboardButtonIsNotVisible() {
        waitUntilElementNotExists(className("dashboard-button"));
        return this;
    }

    public MainPage assertSizingImportButtonIsVisible() {
        return assertButtonExistsAndVisible("sizing-button");
    }

    public MainPage assertSizingImportButtonIsNotVisible() {
        waitUntilElementNotExists(className("sizing-button"));
        return this;
    }

    public MainPage assertButtonExistsAndVisible(String buttonClass) {
        WebElement button = getElementWhenItExists(className(buttonClass));
        waitVisibilityOfElement(button);
        return this;
    }

}
