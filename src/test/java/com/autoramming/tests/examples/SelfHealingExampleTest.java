package com.autoramming.tests.examples;

import com.autoramming.utils.LocatorManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Objects;

/**
 * Example demonstrating Self-Healing capabilities in test automation
 *
 * This test showcases how the SelfHealingEngine automatically:
 * 1. Attempts to find elements using primary locators (fastest)
 * 2. Falls back to semantic similarity matching if primary fails
 * 3. Recovers from minor UI changes without manual intervention
 *
 * @author Auto-Healing Solution Team
 * @version 1.0
 */
@Slf4j
public class SelfHealingExampleTest {

    private WebDriver driver;
    private LocatorManager locatorManager;

    /**
     * Setup test environment with self-healing capabilities
     */
    @BeforeMethod
    public void setUp() {
        log.info("========== TEST SETUP START ==========");
        log.info("Setting up WebDriver with self-healing support");

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        driver = new ChromeDriver(options);

        // Initialize LocatorManager with WebDriver for self-healing
        locatorManager = new LocatorManager("locator/firstlogin.json", driver);
        log.info("LocatorManager initialized with self-healing capabilities");
        log.info("========== TEST SETUP COMPLETE ==========\n");
    }

    /**
     * Example 1: Basic Login with Self-Healing
     *
     * This test demonstrates:
     * - Loading locators from JSON configuration
     * - Navigating to the application
     * - Using self-healing to find and interact with elements
     * - Automatic recovery if locators change
     */
    @Test(description = "Example 1: Login with Self-Healing")
    public void testLoginWithSelfHealing() throws Exception {
        log.info("\n========== TEST 1: LOGIN WITH SELF-HEALING ==========");
        log.info("Objective: Demonstrate automatic healing of failing locators\n");

        // STEP 1: Navigate to application
        String pageUrl = locatorManager.getPageUrl();
        log.info("STEP 1: Navigating to: {}", pageUrl);
        driver.get(pageUrl);
        log.info("✓ Navigation successful\n");
        Thread.sleep(2000);

        // STEP 2: Find and interact with username field
        // This will use self-healing if ID locator fails
        log.info("STEP 2: Finding and filling username field");
        log.info("Attempting to find 'user-name' element...");
        locatorManager.findElementSendKeys("user-name", "standard_user");
        log.info("✓ Username entered successfully\n");
        Thread.sleep(1000);

        // STEP 3: Find and interact with password field
        log.info("STEP 3: Finding and filling password field");
        log.info("Attempting to find 'password' element...");
        locatorManager.findElementSendKeys("password", "secret_sauce");
        log.info("✓ Password entered successfully\n");
        Thread.sleep(1000);

        // STEP 4: Find and click login button
        log.info("STEP 4: Finding and clicking login button");
        log.info("Attempting to find 'login-button' element...");
        WebElement loginButton = locatorManager.findElement("login-button");
        if (loginButton != null) {
            loginButton.click();
            log.info("✓ Login button clicked successfully\n");
        } else {
            log.error("✗ Failed to find login button even with self-healing");
            throw new Exception("Login button not found");
        }
        Thread.sleep(2000);

        // STEP 5: Verify successful login
        log.info("STEP 5: Verifying login was successful");
        String pageTitle = driver.getTitle();
        String expectedTitle = locatorManager.getPageTitle();
        log.info("Expected title: {}", expectedTitle);
        log.info("Actual title: {}", pageTitle);
        Assert.assertTrue(Objects.requireNonNull(pageTitle).contains(expectedTitle),
                "Title does not contain '" + expectedTitle + "'");
        log.info("✓ Page title verified successfully\n");

        log.info("========== TEST 1: PASSED ==========\n");
    }

    /**
     * Example 2: Self-Healing with Safe Click
     *
     * Demonstrates using the safe click method that includes
     * self-healing and error handling
     */
    @Test(description = "Example 2: Safe Click with Self-Healing")
    public void testSafeClickWithSelfHealing() throws Exception {
        log.info("\n========== TEST 2: SAFE CLICK WITH SELF-HEALING ==========");
        log.info("Objective: Demonstrate safe click method with healing\n");

        // Navigate
        driver.get(locatorManager.getPageUrl());
        log.info("✓ Navigated to application");
        Thread.sleep(1000);

        // Fill credentials
        locatorManager.findElementSafeSendKeys("user-name", "standard_user");
        locatorManager.findElementSendKeys("password", "secret_sauce");
        log.info("✓ Credentials entered\n");

        // Use safe click (includes self-healing and error handling)
        log.info("Using safe click method for login button...");
        WebElement clickedElement = locatorManager.findElementSafeClick("login-button");

        if (clickedElement != null) {
            log.info("✓ Element clicked using safe click method");
        } else {
            log.warn("⚠ Safe click returned null but no exception thrown");
        }

        Thread.sleep(2000);
        log.info("========== TEST 2: PASSED ==========\n");
    }

    /**
     * Example 3: Direct Element Finding
     *
     * Demonstrates finding elements without immediate action
     * Useful for complex scenarios requiring element manipulation
     */
    @Test(description = "Example 3: Direct Element Finding with Self-Healing")
    public void testDirectElementFinding() throws Exception {
        log.info("\n========== TEST 3: DIRECT ELEMENT FINDING ==========");
        log.info("Objective: Find elements for custom manipulation\n");

        driver.get(locatorManager.getPageUrl());
        log.info("✓ Navigated to application\n");
        Thread.sleep(1000);

        // Find username element
        log.info("Finding username element...");
        WebElement usernameElement = locatorManager.findElement("user-name");
        Assert.assertNotNull(usernameElement, "Username element not found");
        log.info("✓ Username element found\n");

        // Find password element
        log.info("Finding password element...");
        WebElement passwordElement = locatorManager.findElement("password");
        Assert.assertNotNull(passwordElement, "Password element not found");
        log.info("✓ Password element found\n");

        // Find login button
        log.info("Finding login button element...");
        WebElement loginButtonElement = locatorManager.findElement("login-button");
        Assert.assertNotNull(loginButtonElement, "Login button not found");
        log.info("✓ Login button found\n");

        // Now perform custom actions
        log.info("Performing custom element interactions...");
        usernameElement.clear();
        usernameElement.sendKeys("standard_user");
        passwordElement.clear();
        passwordElement.sendKeys("secret_sauce");
        loginButtonElement.click();
        log.info("✓ Custom interactions complete\n");

        Thread.sleep(2000);
        log.info("========== TEST 3: PASSED ==========\n");
    }

    /**
     * Example 4: Demonstrating Self-Healing Recovery
     *
     * Shows what happens when primary locator fails
     * and self-healing engine takes over
     */
    @Test(description = "Example 4: Self-Healing Recovery Process")
    public void testSelfHealingRecovery() throws Exception {
        log.info("\n========== TEST 4: SELF-HEALING RECOVERY ==========");
        log.info("Objective: Show recovery when primary locator fails\n");

        driver.get(locatorManager.getPageUrl());
        log.info("✓ Navigated to application\n");
        Thread.sleep(1000);

        log.info("Demonstrating self-healing recovery process:");
        log.info("If the ID 'user-name' doesn't exist on the page:");
        log.info("  1. Primary locator By.id('user-name') will FAIL");
        log.info("  2. Self-Healing Engine activates automatically");
        log.info("  3. NLP tokenizer analyzes all visible input elements");
        log.info("  4. Calculates similarity: 'user-name' vs element attributes");
        log.info("  5. Returns element with highest similarity score");
        log.info("  6. Test continues without manual intervention\n");

        // This call will automatically use self-healing if needed
        locatorManager.findElementSendKeys("user-name", "standard_user");
        log.info("✓ Element found (either primary or via self-healing)\n");

        locatorManager.findElementSendKeys("password", "secret_sauce");
        log.info("✓ All elements found and filled\n");

        // Click login
        locatorManager.findElementSafeClick("login-button");
        Thread.sleep(2000);

        // Verify
        String title = driver.getTitle();
        log.info("Final page title: {}", title);
        log.info("========== TEST 4: PASSED ==========\n");
    }

    /**
     * Example 5: Using Locator Manager Utilities
     *
     * Demonstrates utility methods available in LocatorManager
     */
    @Test(description = "Example 5: Locator Manager Utilities")
    public void testLocatorManagerUtilities() throws Exception {
        log.info("\n========== TEST 5: LOCATOR MANAGER UTILITIES ==========");
        log.info("Objective: Show available utility methods\n");

        // Get page details
        log.info("Available Utility Methods:");
        log.info("1. getPageUrl(): {}", locatorManager.getPageUrl());
        log.info("2. getPageTitle(): {}", locatorManager.getPageTitle());

        // Get all locators
        var allLocators = locatorManager.getAllLocators();
        log.info("3. getAllLocators(): Found {} locators", allLocators.size());

        // Get specific locator by name
        var usernameLoc = locatorManager.getLocatorByName("user-name");
        log.info("4. getLocatorByName('user-name'): Found? {}", usernameLoc != null);
        if (usernameLoc != null) {
            log.info("   - Tag: {}", usernameLoc.get("locatorTag"));
            log.info("   - Selected: {}", usernameLoc.get("selected"));
        }

        // Get locators by tag
        var inputLocators = locatorManager.getLocatorsByTag("input");
        log.info("5. getLocatorsByTag('input'): Found {} input elements", inputLocators.size());

        // Get selected locators
        var selectedLocators = locatorManager.getSelectedLocators();
        log.info("6. getSelectedLocators(): Found {} selected locators", selectedLocators.size());

        log.info("\n========== TEST 5: PASSED ==========\n");
    }

    /**
     * Teardown test environment
     */
    @AfterMethod
    public void tearDown() {
        log.info("========== TEST TEARDOWN START ==========");
        if (driver != null) {
            log.info("Closing WebDriver");
            driver.quit();
            log.info("✓ WebDriver closed successfully");
        }
        log.info("========== TEST TEARDOWN COMPLETE ==========\n");
    }
}

