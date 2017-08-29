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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class MenuFilters extends AbstractUiFragment {
    @FindBy(tagName = "aspects-filter")
    private WebElement aspectsFilterButton;

    @FindBy(css = ".aspect-item-filter.config-item-title")
    private List<WebElement> aspectItemFilters;

    public MenuFilters(WebDriver webDriver) {
        super(webDriver);
    }

    public MenuFilters openAspectsFilter() {
        waitVisibilityOfElement(aspectsFilterButton);
        aspectsFilterButton.click();
        return this;
    }

    public MenuFilters clickCheckAllFilter(String filterName) {
        WebElement checkAll = aspectItemFilters.stream()
            .filter(i -> filterName.equals(i.getText()))
            .map(i -> i.findElement(By.id("checkAll")))
            .findFirst().orElse(null);

        if (checkAll == null)
            throw new IllegalArgumentException("Element \"checkAll\" of " + filterName + " filter  not found");

        waitVisibilityOfElement(checkAll);
        checkAll.click();
        return this;
    }

    public void closeMenuFilters() {
        WebElement title = webDriver.findElement(By.className("title"));
        title.click();
        waitInvisibilityOfElement(aspectsFilterButton);
    }
}
