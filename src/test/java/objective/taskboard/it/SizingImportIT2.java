package objective.taskboard.it;

import static org.jboss.arquillian.graphene.Graphene.waitGui;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.Graphene;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;

import objective.taskboard.it.SizingImportIT2.SizingImportModal;

@RunWith(Arquillian.class)
public class SizingImportIT2 {

    @Drone
    private WebDriver browser;
    
    @Page
    private MainPage2 mainPage;
    
    @Page
    private LoginPage2 loginPage;

    @Test
    public void test() {
        browser.get(AbstractUIIntegrationTest.getSiteBase() + "/login");
        
        loginPage.login("foo", "bar");
        
        SizingImportModal openSizingImport = mainPage.openSizingImport();
        
        openSizingImport.assertTitle("Checking authorization or spreadsheet format");
    }

    public static class MainPage2 {
        @FindBy(css=".sizing-button")
        private WebElement buttonOpenSizing;
        
        @FindBy(id="sizingimport")
        private SizingImportModal sizingImportModal;

        public SizingImportModal openSizingImport() {
            waitGui().until().element(this.buttonOpenSizing).is().visible();
            buttonOpenSizing.click();
            
            return sizingImportModal;
        }
    }
    
    public static class SizingImportModal {
        
        @FindBy(css=".step__title")
        private WebElement title;
        
        public void assertTitle(String title) {
            waitGui().until().element(this.title).text().equalTo(title);
        }
    }

    public static class LoginPage2 {
        @FindBy(id="username")
        private WebElement username;
    
        @FindBy(id="password")
        private WebElement password;
    
        @FindBy(id="login")
        private WebElement submit;

        public void login(String username, String password) {
            waitGui().until().element(this.username).is().visible();
            
            this.username.sendKeys(username);
            this.password.sendKeys(password);
            this.submit.click();
        }   
    }

}
