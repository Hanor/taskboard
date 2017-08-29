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

import static org.junit.Assert.assertEquals;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ErrorToast extends AbstractUiFragment {

    @FindBy(id = "toastError")
    private WebElement errorToast;

    public ErrorToast(WebDriver driver) {
        super(driver);
    }

    public void assertErrorMessage(String errorMessage) {
        waitVisibilityOfElement(errorToast);
        WebElement spanErrorMessage = errorToast.findElement(By.cssSelector("span.taskboard-home"));
        assertEquals("Error message", errorMessage, spanErrorMessage.getText());
    }

    public void assertErrorToastIsInvisible() {
        waitInvisibilityOfElement(errorToast);
    }

    public void close() {
        waitVisibilityOfElement(errorToast);
        errorToast.findElement(By.tagName("paper-button")).click();
    }

}
