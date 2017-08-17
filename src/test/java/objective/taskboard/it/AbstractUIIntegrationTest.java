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

import java.io.File;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Rule;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.safaribooks.junitattachments.CaptureFile;
import com.safaribooks.junitattachments.RecordAttachmentRule;
import org.openqa.selenium.firefox.FirefoxOptions;

public abstract class AbstractUIIntegrationTest extends AbstractIntegrationTest {
    protected WebDriver webDriver;

    public void setup() {
        if (System.getProperty("webdriver.gecko.driver") == null)
            System.setProperty("webdriver.gecko.driver", "drivers/linux/marionette/64bit/geckodriver");
        
        
        if (!new File("drivers/linux/marionette/64bit/geckodriver").exists()) 
            throw new IllegalStateException("To run integration tests, you must run 'mvn clean install' at least once to download gecko driver");
        
        super.setup();

        FirefoxOptions options = new FirefoxOptions();
        options.addPreference("dom.file.createInChild", true);
        options.addPreference("browser.link.open_newwindow", 3);
        options.addPreference("browser.link.open_newwindow.restriction", 2);
        webDriver = new FirefoxDriver(options);
        webDriver.manage().window().setSize(new Dimension(1280,1080));
    }
    
    @CaptureFile(extension = "html")
    public String capturedDom = null;

    @CaptureFile(extension = "png")
    public byte[] capturePage = null;

    @After
    public void cleanupThread() {
        if (webDriver == null) return;
        try {
            // capture the dom
            capturedDom = webDriver.getPageSource();
    
            // capture a screenshot
            if (webDriver instanceof TakesScreenshot) {
                capturePage = ((TakesScreenshot) webDriver)
                        .getScreenshotAs(OutputType.BYTES);
            }
    
            webDriver.quit();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Rule
    public RecordAttachmentRule recordArtifactRule = new RecordAttachmentRule(this);


    protected void switchToFirstTab() {
        webDriver.findElement(By.cssSelector("body")).sendKeys(Keys.CONTROL +"\t");
        ArrayList<String> tabs = new ArrayList<>(webDriver.getWindowHandles());
        webDriver.switchTo().window(tabs.get(0));
    }

    protected ArrayList<String> createAndSwitchToNewTab() {
        ((JavascriptExecutor)webDriver).executeScript("window.open('about:blank','_blank');");
        ArrayList<String> tabs = new ArrayList<>(webDriver.getWindowHandles());

        webDriver.switchTo().window(tabs.get(1));
        return tabs;
    }
}
