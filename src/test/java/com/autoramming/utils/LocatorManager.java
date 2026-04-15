package com.autoramming.utils;

import com.autoramming.heal.SelfHealingEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class LocatorManager {

    private static final Logger logger = LoggerFactory.getLogger(LocatorManager.class);
    private Map<String, Object> locatorData;
    private final String filePath;
    private WebDriver driver;

    public LocatorManager(String filePath) {
        this.filePath = filePath;
        loadLocators();
    }

    public LocatorManager(String filePath, WebDriver driver) {
        this.filePath = filePath;
        this.driver = driver;
        loadLocators();
    }

    /**
     * Set WebDriver for self-healing capabilities
     */
    public void setWebDriver(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Loads locators from the JSON file
     */
    private void loadLocators() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
            if (inputStream != null) {
                this.locatorData = objectMapper.readValue(inputStream, Map.class);
                logger.info("Locators loaded successfully from: {}", filePath);
            } else {
                // Fallback to file path
                this.locatorData = objectMapper.readValue(new File(filePath), Map.class);
                logger.info("Locators loaded successfully from: {}", filePath);
            }
        } catch (Exception e) {
            logger.error("Failed to load locators from {}", filePath, e);
            throw new RuntimeException("Failed to load locators", e);
        }
    }

    /**
     * Get all locators
     */
    public List<Map<String, Object>> getAllLocators() {
        return (List<Map<String, Object>>) locatorData.get("locators");
    }

    /**
     * Get a specific locator by name
     */
    public Map<String, Object> getLocatorByName(String locatorName) {
        return getAllLocators().stream()
                .filter(loc -> locatorName.equals(loc.get("locatorName")))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get locators by HTML tag
     */
    public List<Map<String, Object>> getLocatorsByTag(String tag) {
        return getAllLocators().stream()
                .filter(loc -> tag.equals(loc.get("locatorTag")))
                .collect(Collectors.toList());
    }

    /**
     * Get locators where selected is true
     */
    public List<Map<String, Object>> getSelectedLocators() {
        return getAllLocators().stream()
                .filter(loc -> Boolean.TRUE.equals(loc.get("selected")))
                .collect(Collectors.toList());
    }

    /**
     * Convert locator to Selenium By object
     */
    public By getByLocator(String locatorName) {
        Map<String, Object> locator = getLocatorByName(locatorName);
        if (locator == null) {
            logger.warn("Locator not found: {}", locatorName);
            return null;
        }

        List<Map<String, Object>> properties = (List<Map<String, Object>>) locator.get("properties");
        if (properties == null || properties.isEmpty()) {
            logger.warn("No properties found for locator: {}", locatorName);
            return null;
        }

        // Prioritize by property type: id > name > class > xpath
        for (Map<String, Object> prop : properties) {
            String propertyType = (String) prop.get("propertyType");
            String propertyValue = (String) prop.get("propertyValue");

            if ("id".equalsIgnoreCase(propertyType)) {
                logger.debug("Using ID locator for {}: {}", locatorName, propertyValue);
                return By.id(propertyValue);
            }
        }

        for (Map<String, Object> prop : properties) {
            String propertyType = (String) prop.get("propertyType");
            String propertyValue = (String) prop.get("propertyValue");

            if ("name".equalsIgnoreCase(propertyType)) {
                logger.debug("Using Name locator for {}: {}", locatorName, propertyValue);
                return By.name(propertyValue);
            }
        }

        for (Map<String, Object> prop : properties) {
            String propertyType = (String) prop.get("propertyType");
            String propertyValue = (String) prop.get("propertyValue");

            if ("class".equalsIgnoreCase(propertyType)) {
                logger.debug("Using CSS Selector locator for {}: {}", locatorName, propertyValue);
                return By.cssSelector("." + propertyValue);
            }
        }

        Map<String, Object> firstProp = properties.get(0);
        String propertyType = (String) firstProp.get("propertyType");
        String propertyValue = (String) firstProp.get("propertyValue");
        logger.debug("Using {} locator for {}: {}", propertyType, locatorName, propertyValue);
        return By.xpath("//*[@" + propertyType + "='" + propertyValue + "']");
    }

    /**
     * Find element with self-healing capability
     * First tries to find using the locator from JSON
     * If fails, uses SelfHealingEngine to find the best match
     */
    public WebElement findElement(String locatorName) {
        if (driver == null) {
            throw new IllegalStateException("WebDriver not set. Call setWebDriver() first or use constructor with driver parameter.");
        }

        Map<String, Object> locator = getLocatorByName(locatorName);
        if (locator == null) {
            logger.error("Locator not found: {}", locatorName);
            return null;
        }

        // Try primary locator first
        try {
            By byLocator = getByLocator(locatorName);
            if (byLocator != null) {
                WebElement element = driver.findElement(byLocator);
                logger.info("Element found using primary locator: {}", locatorName);
                return element;
            }
        } catch (Exception e) {
            logger.warn("Failed to find element using primary locator '{}'. Attempting self-healing...", locatorName, e);
        }

        // Self-healing fallback
        String tag = (String) locator.get("locatorTag");
        logger.info("Invoking self-healing for locator: {} with tag: {}", locatorName, tag);

        try {
            WebElement healedElement = SelfHealingEngine.findBestMatch(driver, locatorName, tag);
            if (healedElement != null) {
                logger.info("Self-healing successful! Found element for: {}", locatorName);
                return healedElement;
            }
        } catch (Exception e) {
            logger.error("Self-healing failed for locator: {}", locatorName, e);
        }

        logger.error("Failed to find element for locator: {} using both primary and self-healing methods", locatorName);
        return null;
    }

    /**
     * Find element and perform click with self-healing capability
     */
    public WebElement findElementSafeClick(String locatorName) {
        WebElement element = findElement(locatorName);
        if (element != null) {
            try {
                element.click();
                logger.info("Successfully clicked on element: {}", locatorName);
            } catch (Exception e) {
                logger.error("Failed to click on element: {}", locatorName, e);
            }
        }
        return element;
    }



    /**
     * Find element and perform click with self-healing capability
     */
    public WebElement findElementSafeSendKeys(String locatorName, String value) {
        WebElement element = findElement(locatorName);
        if (element != null) {
            try {
                element.sendKeys(value);
                logger.info("Successfully clicked on element: {}", locatorName);
            } catch (Exception e) {
                logger.error("Failed to click on element: {}", locatorName, e);
            }
        }
        return element;
    }

    /**
     * Find element and send keys with self-healing capability
     */
    public void findElementSendKeys(String locatorName, String keys) {
        WebElement element = findElement(locatorName);
        if (element != null) {
            try {
                element.clear();
                element.sendKeys(keys);
                logger.info("Successfully sent keys to element: {}", locatorName);
            } catch (Exception e) {
                logger.error("Failed to send keys to element: {}", locatorName, e);
            }
        }
    }

    /**
     * Get page details
     */
    public Map<String, Object> getPageDetails() {
        return (Map<String, Object>) locatorData.get("pageDetails");
    }

    /**
     * Get page URL
     */
    public String getPageUrl() {
        Map<String, Object> pageDetails = getPageDetails();
        return (String) pageDetails.get("pageUrl");
    }

    /**
     * Get page title
     */
    public String getPageTitle() {
        Map<String, Object> pageDetails = getPageDetails();
        return (String) pageDetails.get("pageTitle");
    }

    /**
     * Print all locators (for debugging)
     */
    public void printAllLocators() {
        logger.info("=== All Locators ===");
        getAllLocators().forEach(locator -> {
            logger.info("Locator Name: {}", locator.get("locatorName"));
            logger.info("Locator Tag: {}", locator.get("locatorTag"));
            logger.info("Selected: {}", locator.get("selected"));
            List<Map<String, Object>> properties = (List<Map<String, Object>>) locator.get("properties");
            if (properties != null) {
                properties.forEach(prop ->
                    logger.info("  - {} = {}", prop.get("propertyType"), prop.get("propertyValue"))
                );
            }
            logger.info("---");
        });
    }
}

