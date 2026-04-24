package com.autoramming.tests;

import com.autoramming.utils.LocatorManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Objects;

public class LoginSauceTest {

    private WebDriver driver;
        private LocatorManager locatorManager;
    private static final Logger log = LoggerFactory.getLogger(LoginSauceTest.class);

        @BeforeMethod
        public void setUp() {
            log.info("Setting up WebDriver");
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            driver = new ChromeDriver(options);

            // Initialize LocatorManager with WebDriver for self-healing
            locatorManager = new LocatorManager("locator/firstlogin.json", driver);
            log.info("LocatorManager initialized with self-healing capabilities");
        }

        @Test
        public void testSample() throws Exception{
            log.info("Test started");

            // Get page URL from locators
            String pageUrl = locatorManager.getPageUrl();
            driver.get(pageUrl);
            log.info("Navigated to: {}", pageUrl);

            Thread.sleep(2000);

            // Use self-healing element finding
            log.info("Attempting to find and interact with login elements using self-healing...");

            // Find username field with self-healing capability
            locatorManager.findElementSendKeys("user-name", "standard_user");
            log.info("Successfully entered username");

            // Find password field with self-healing capability
            locatorManager.findElementSendKeys("password", "secret_sauce");
            log.info("Successfully entered password");

            // Find and click login button with self-healing capability
            WebElement loginButton = locatorManager.findElement("login-button");
            if (loginButton != null) {
                loginButton.click();
                log.info("Successfully clicked login button");
            } else {
                log.error("Failed to find login button even with self-healing");
                throw new Exception("Login button not found");
            }

            // Add assertions or interactions here
            String expectedTitle = locatorManager.getPageTitle();
            Assert.assertTrue(Objects.requireNonNull(driver.getTitle()).contains(expectedTitle),
                    "Title does not contain '" + expectedTitle + "'");
            log.info("Test completed successfully");
            Thread.sleep(2000);
        }

        @AfterMethod
        public void tearDown() {
            log.info("Tearing down WebDriver");
            if (driver != null) {
                driver.quit();
            }
        }

}
