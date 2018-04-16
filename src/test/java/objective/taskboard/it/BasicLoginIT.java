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

import org.junit.Test;

public class BasicLoginIT extends AbstractUIWithCoverageIntegrationTest {
    @Test
    public void givenAdminUser_whenLogin_thenHasAdminAccess() {
        LoginPage loginPage = LoginPage.to(webDriver);
        loginPage.login("foo", "bar");

        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.waitUserLabelToBe("Foo");
        mainPage.assertFollowupButtonIsVisible()
            .assertDashboardButtonIsVisible()
            .assertSizingImportButtonIsVisible();
    }

    @Test
    public void givenDeveloperUser_whenLogin_thenHasDeveloperAccess() {
        LoginPage loginPage = LoginPage.to(webDriver);
        loginPage.login("thomas.developer", "thomas.developer");

        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.waitUserLabelToBe("Thomas.developer");
        mainPage.assertFollowupButtonIsNotVisible()
            .assertDashboardButtonIsVisible()
            .assertSizingImportButtonIsNotVisible();
    }

    @Test
    public void givenCustomerUser_whenLogin_thenHasCustomerAccess() {
        LoginPage loginPage = LoginPage.to(webDriver);
        loginPage.login("albert.customer", "albert.customer");

        MainPage mainPage = MainPage.produce(webDriver);
        mainPage.waitUserLabelToBe("Albert.customer");
        mainPage.assertFollowupButtonIsNotVisible()
            .assertDashboardButtonIsNotVisible()
            .assertSizingImportButtonIsNotVisible();
    }
}
