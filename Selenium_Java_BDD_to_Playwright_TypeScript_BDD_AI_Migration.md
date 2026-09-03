# Selenium Java BDD → Playwright TypeScript BDD Migration

## AI-Assisted Migration Using GitHub Copilot

> **Context:** This document captures the migration challenges and
> mitigation strategies discussed for migrating a Selenium Java BDD
> framework to Playwright TypeScript BDD using GitHub Copilot as an
> AI-assisted migration accelerator.

## Important Framing

The migration is **not** being performed manually. GitHub Copilot is
being used to accelerate the conversion.

The key story is:

> **"We used GitHub Copilot as an AI-assisted migration accelerator, but
> we had to establish architectural rules, validate Copilot-generated
> code, and resolve framework-level issues that Copilot could not
> reliably infer."**

A technically credible migration story should not be presented as:

> "Copilot migrated our Selenium framework."

Instead:

> **"We used Copilot to accelerate the migration, but the migration was
> governed by an automation architecture and validated through
> compilation, BDD generation, functional execution, regression
> comparison and code review."**

---

# 1. Migration Is Not Just an API Replacement

The biggest mistake is treating:

> Selenium Java → Playwright TypeScript = API replacement

It is not.

The migration changes the:

- execution model
- programming language
- synchronization model
- browser lifecycle
- BDD integration
- reporting
- CI/CD execution
- parallelization model
- test-data strategy
- automation architecture

The migration therefore needs architectural governance rather than
simple line-by-line translation.

---

# 2. Cucumber.js vs `playwright-bdd`

One of the first architectural decisions is choosing the BDD
integration.

### Existing Selenium Java architecture

```text
Feature
   ↓
Cucumber-JVM
   ↓
Step Definitions
   ↓
Page Objects
   ↓
Selenium WebDriver
   ↓
Browser
```

### Option A --- Cucumber.js

```text
Feature
   ↓
Cucumber.js
   ↓
Step Definitions
   ↓
Playwright
   ↓
Browser
```

### Option B --- playwright-bdd

```text
Feature
   ↓
playwright-bdd
   ↓
Playwright Test Runner
   ↓
Fixtures / Steps
   ↓
Playwright
   ↓
Browser
```

`playwright-bdd` converts Gherkin scenarios into Playwright tests and
provides integration with Playwright's execution model.

### Challenge

Copilot can generate a technically valid Cucumber.js implementation even
when the target architecture is supposed to use `playwright-bdd`.

### How we overcame it

We froze the target architecture before migration:

```text
BDD Layer
   ↓
playwright-bdd
   ↓
Step Layer
   ↓
Workflow / Business Layer
   ↓
Page Component Layer
   ↓
Playwright
```

We did not allow developers or Copilot to choose different BDD
approaches for different modules.

### Real GitHub evidence

The `playwright-bdd` repository and issues discuss its relationship with
Cucumber and the architectural changes made to reduce dependence on the
Cucumber runner.

---

# 3. Synchronous Java → Async TypeScript

This is one of the first major coding challenges.

### Selenium Java

```java
loginPage.enterUsername("user");
loginPage.enterPassword("password");
loginPage.clickLogin();
```

### Playwright TypeScript

```typescript
await loginPage.enterUsername("user");
await loginPage.enterPassword("password");
await loginPage.clickLogin();
```

The `async/await` requirement propagates through the application:

```text
Step
 ↓
Workflow
 ↓
Page Object
 ↓
Utility
 ↓
API/DB helper
```

### Challenge

A missing `await` can result in:

```text
Promise<string>
```

being used where:

```text
string
```

was expected.

### How we overcame it

We made the following rules part of the Copilot migration instructions:

> Every Playwright operation must be awaited unless the Promise is
> deliberately handled.

We also enabled strict TypeScript validation.

```json
{
  "compilerOptions": {
    "strict": true
  }
}
```

Migration loop:

```text
Copilot generates code
       ↓
TypeScript type check
       ↓
Compile errors
       ↓
Fix
       ↓
Test execution
```

---

# 4. WebDriver Lifecycle → Browser/Context/Page

A typical Selenium framework can have:

```java
@Before
public void setup() {
    driver = new ChromeDriver();
}
```

and:

```java
@After
public void tearDown() {
    driver.quit();
}
```

Enterprise Selenium frameworks may additionally use:

```text
BaseTest
   ↓
DriverManager
   ↓
ThreadLocal<WebDriver>
```

### Challenge

Simply recreating `DriverManager` and global `Page` objects in
TypeScript carries Selenium architecture into Playwright.

### How we overcame it

We redesigned the lifecycle:

```text
Worker
 ↓
Browser
 ↓
BrowserContext
 ↓
Page
 ↓
Page Objects
```

We avoided global/static Page objects.

Bad pattern:

```typescript
export let page: Page;
```

Preferred model:

```text
Scenario
 ↓
Fixture
 ↓
Page
 ↓
Page Object
```

Playwright BrowserContexts provide isolated browser sessions, and
Playwright Test provides isolated contexts for tests.

---

# 5. `ThreadLocal<WebDriver>` → Playwright Workers and Contexts

### Existing Selenium approach

```java
ThreadLocal<WebDriver>
```

was often used to isolate drivers for parallel execution.

### Challenge

Copilot may attempt to reproduce this with global or thread-like
constructs in TypeScript.

### How we overcame it

We did not recreate `ThreadLocal`.

We adopted Playwright's execution model:

```text
Worker
 └── Browser
      └── Context
           └── Page
```

This gives a cleaner isolation model and avoids unnecessary custom
driver-management infrastructure.

---

# 6. Selenium `WebDriverWait` / `ExpectedConditions`

Existing Selenium code might contain:

```java
WebDriverWait wait =
    new WebDriverWait(driver, Duration.ofSeconds(30));

wait.until(
    ExpectedConditions.visibilityOfElementLocated(
        By.id("submit")
    )
);
```

### Challenge

A mechanical Copilot conversion might produce:

```typescript
await page.waitForTimeout(2000);
```

or unnecessary explicit waits.

### Why this is a problem

Playwright already performs actionability checks for actions such as
click, including checks related to visibility, stability, receiving
events and enabled state.

Playwright assertions also retry automatically.

### How we overcame it

We mapped waits based on **what the Selenium code was actually waiting
for**.

Selenium Playwright

---

`visibilityOfElementLocated` Locator/action
`elementToBeClickable` `locator.click()`
`presenceOfElementLocated` Locator
`textToBePresentInElement` `expect(locator).toContainText()`
`invisibilityOfElementLocated` `expect(locator).toBeHidden()`
`Thread.sleep()` Remove/rethink synchronization
Custom polling Locator/assertion/event-based wait

### Migration rule

> **Do not convert Selenium explicit waits or `Thread.sleep()` into
> Playwright `waitForTimeout`. Determine the condition the Selenium code
> was waiting for and implement that condition using Playwright
> locators, assertions, or event-based waits.**

---

# 7. `Thread.sleep()` → Condition-Based Synchronization

Legacy Selenium frameworks may contain:

```java
Thread.sleep(2000);
```

A mechanical conversion gives:

```typescript
await page.waitForTimeout(2000);
```

### Challenge

The test may pass, but the underlying synchronization problem remains.

### How we overcame it

We identified the actual condition.

Instead of:

```typescript
await page.waitForTimeout(3000);
```

use:

```typescript
await expect(page.getByText("Order Created")).toBeVisible();
```

or wait for the relevant network/API event when that is the real
synchronization point.

---

# 8. XPath → Playwright Locator Strategy

Legacy Selenium:

```java
By.xpath("//button[contains(text(),'Submit')]");
```

Mechanical migration:

```typescript
page.locator("//button[contains(text(),'Submit')]");
```

This may work, but it does not take advantage of Playwright's
recommended locator model.

### Preferred migration

```typescript
page.getByRole("button", { name: "Submit" });
```

Other preferred strategies:

```typescript
page.getByText("Submit");
page.getByLabel("Username");
page.getByPlaceholder("Enter username");
page.getByTestId("submit-order");
```

### Locator priority

```text
1. getByRole()
2. getByLabel()
3. getByPlaceholder()
4. getByText()
5. getByTestId()
6. CSS
7. XPath — last resort
```

### How we overcame it

We asked Copilot to **re-evaluate** Selenium locators rather than
blindly translate them.

Migration instruction:

> Convert Selenium locators to robust Playwright locators. Prefer role,
> label, placeholder, text and test-id locators. Use CSS or XPath only
> when necessary.

---

# 9. PageFactory → Playwright Locators

Legacy Java:

```java
@FindBy(id = "username")
WebElement username;
```

and:

```java
PageFactory.initElements(driver, this);
```

### Challenge

Trying to recreate PageFactory in TypeScript creates unnecessary
framework complexity.

### How we overcame it

We used Playwright Locators directly:

```typescript
export class LoginPage {
  constructor(private readonly page: Page) {}

  readonly username = this.page.getByLabel("Username");

  readonly password = this.page.getByLabel("Password");

  readonly loginButton = this.page.getByRole("button", { name: "Login" });

  async login(username: string, password: string) {
    await this.username.fill(username);
    await this.password.fill(password);
    await this.loginButton.click();
  }
}
```

### Rule

> **Do not create a PageFactory equivalent. Use Playwright Locators as
> the element abstraction.**

---

# 10. Stale Element Handling

Selenium commonly works with `WebElement` objects:

```java
WebElement element = driver.findElement(...);
```

Dynamic DOM updates can result in:

```text
StaleElementReferenceException
```

### Playwright difference

A Locator represents how to find an element and resolves it when an
action is performed.

### Challenge

Developers may still create custom element caching with `ElementHandle`,
reproducing Selenium-style problems.

### How we overcame it

Prefer:

```typescript
const button = page.getByRole("button", { name: "Submit" });

await button.click();
```

instead of unnecessarily caching DOM handles.

---

# 11. Selenium Actions API

Selenium:

```java
Actions actions = new Actions(driver);

actions.moveToElement(element)
       .click()
       .perform();
```

### Playwright

```typescript
await element.hover();
await element.click();
```

Similarly:

```java
actions.doubleClick(element).perform();
```

becomes:

```typescript
await element.dblclick();
```

### Migration lesson

Do not recreate Selenium's `Actions` abstraction when Playwright already
provides higher-level APIs.

---

# 12. JavaScriptExecutor

Selenium:

```java
JavascriptExecutor js =
    (JavascriptExecutor) driver;

js.executeScript(
    "arguments[0].click();", element
);
```

Possible Playwright translation:

```typescript
await page.evaluate(() => {
  // ...
});
```

### Challenge

Copilot may use `evaluate()` too frequently because it is an obvious
JavaScript replacement.

### How we overcame it

Normal Playwright actions are preferred:

```typescript
await locator.click();
```

Use:

```typescript
await page.evaluate(...);
```

only when browser-side JavaScript is genuinely required.

The reason is that normal Playwright actions retain actionability
checks.

---

# 13. Frames

Selenium:

```java
driver.switchTo().frame("paymentFrame");
```

Playwright:

```typescript
const frame = page.frameLocator("#paymentFrame");

await frame.getByLabel("Card Number").fill("...");
```

### Challenge

The old framework may contain:

```text
switchToFrame()
switchToDefaultContent()
```

throughout many Page Objects.

### How we overcame it

Frame-specific interactions were encapsulated inside the relevant
page/component rather than creating global frame-switching utilities.

---

# 14. Multiple Windows / Tabs / Popups

Selenium often uses:

```java
String parent = driver.getWindowHandle();

Set<String> windows =
    driver.getWindowHandles();
```

and then switches window handles.

### Playwright

```typescript
const popupPromise = page.waitForEvent("popup");

await page.getByText("Open").click();

const popup = await popupPromise;

await popup.waitForLoadState();
```

### Challenge

The Playwright event needs to be captured before triggering the action.

### How we overcame it

We redesigned window handling around Playwright events instead of
reproducing Selenium's window-handle model.

---

# 15. Browser Alerts

Selenium:

```java
driver.switchTo().alert().accept();
```

Playwright:

```typescript
page.on("dialog", async (dialog) => {
  await dialog.accept();
});
```

### Migration change

Generic utilities such as:

```text
acceptAlert()
dismissAlert()
getAlertText()
```

were redesigned around Playwright's event-based dialog handling.

---

# 16. File Upload / Download

Legacy Selenium frameworks may use:

```text
<input type=file>
Robot
AutoIT
OS-level file chooser
```

### Playwright upload

```typescript
await page.getByLabel("Upload file").setInputFiles(filePath);
```

### Playwright download

```typescript
const downloadPromise = page.waitForEvent("download");

await page.getByText("Download").click();

const download = await downloadPromise;

await download.saveAs(targetPath);
```

### Challenge

The old framework may have extensive OS-level utilities.

### How we overcame it

We replaced those utilities with Playwright-native upload/download APIs
where possible.

---

# 17. Authentication

A Selenium suite may perform login in every scenario:

```text
Given I open application
When I enter username
And I enter password
And I click login
```

### Challenge

Repeated login makes large suites slower and creates additional points
of failure.

### How we overcame it

We used Playwright authentication state where appropriate:

```text
Authentication
      ↓
storageState
      ↓
Scenario
```

instead of:

```text
Every scenario
      ↓
Login
      ↓
Application
```

Authentication state contains credentials/cookies capable of
impersonating an account, so it must not be committed to source control.

---

# 18. Cucumber World → Playwright Fixtures

A Selenium Java BDD framework may use:

```text
Scenario
 ↓
World / Context
 ↓
Driver
 ↓
Page Objects
```

### Challenge

Directly recreating the World object can result in shared state and
complicated lifecycle handling.

### Target

Use fixtures and dependency injection:

```typescript
export const test = base.extend<{
  loginPage: LoginPage;
}>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },
});
```

Then BDD steps consume the appropriate fixture/context.

Playwright fixtures are designed to establish test dependencies and
maintain isolation.

---

# 19. Cucumber Hooks → Playwright Lifecycle

Java:

```text
@Before
@BeforeAll
@After
@AfterAll
```

### Challenge

A simple annotation-to-annotation translation is unsafe.

The semantics of:

```text
Before
BeforeAll
```

need to be understood in the context of:

```text
Scenario
Worker
Entire run
```

### How we overcame it

We classified setup:

```text
Global environment setup
        ↓
Worker setup
        ↓
Scenario setup
        ↓
Page/component setup
```

Each old hook was mapped based on **purpose and lifecycle**, not merely
annotation name.

Real `playwright-bdd` discussions/issues cover hook and lifecycle
behavior, especially around parallel workers.

---

# 20. `BeforeAll` + Parallel Execution

Suppose:

```text
BeforeAll
  ↓
Create test data
```

and the suite uses multiple workers.

### Challenge

A setup that was assumed to be global may execute once per worker,
causing duplicated setup or conflicting data.

### How we overcame it

We explicitly distinguished:

```text
Once per entire run
Once per worker
Once per scenario
```

We avoided assuming that `BeforeAll` automatically means "once globally"
under parallel execution.

---

# 21. Shared Test Data Broke Parallel Execution

A sequential Selenium suite may use:

```text
Customer ID = 10001
```

for many scenarios.

Parallel Playwright execution can create:

```text
Scenario A → modifies 10001
Scenario B → modifies 10001
Scenario C → deletes 10001
Scenario D → searches 10001
```

### Result

Random failures and race conditions.

### How we overcame it

We introduced unique test data:

```typescript
const customerId = `customer-${testInfo.testId}`;
```

or created data through API/database fixtures.

### Principle

> **Tests running in parallel must not depend on shared mutable test
> data unless that resource is deliberately controlled.**

---

# 22. Parallel Execution Exposed Hidden Dependencies

Old Selenium suites may have been sequential for years.

Once Playwright workers were introduced:

```text
Worker 1 → Scenario A
Worker 2 → Scenario B
Worker 3 → Scenario C
Worker 4 → Scenario D
```

hidden dependencies became visible.

Examples:

```text
Scenario B assumes Scenario A created user
Scenario C assumes Scenario B changed status
Scenario D uses same account
```

### How we overcame it

Scenarios were categorized as:

```text
Independent
Dependent
Shared-account
Data-sensitive
Environment-sensitive
```

Dependent scenarios were refactored so each establishes the state it
requires.

Where shared resources genuinely cannot be parallelized, worker limits
can be controlled.

---

# 23. TypeScript Compilation Exposed Migration Problems

The new framework introduced TypeScript's type system.

Typical errors included:

```text
string | undefined
```

versus:

```text
string
```

and:

```text
Promise<string>
```

being used as:

```text
string
```

### How we overcame it

We enabled:

```text
strict TypeScript
ESLint
Prettier
```

and made type checking part of CI.

Migration pipeline:

```text
Code generated by Copilot
        ↓
Type check
        ↓
Lint
        ↓
BDD generation
        ↓
Test execution
```

---

# 24. Maven/Java Dependencies → npm Dependencies

Existing:

```text
pom.xml
 ├── selenium-java
 ├── cucumber-java
 ├── testng
 ├── rest-assured
 └── other Java libraries
```

New:

```text
package.json
package-lock.json
tsconfig.json
playwright.config.ts
```

### Challenge

Dependency compatibility became a new concern.

### How we overcame it

We controlled versions instead of blindly using:

```bash
npm install latest
```

The compatibility set was treated as one unit:

```text
Node
Playwright
@playwright/test
playwright-bdd
TypeScript
Module configuration
```

---

# 25. ESM/CommonJS Problems

TypeScript + Node + Cucumber/Playwright-BDD can introduce module-system
issues.

Real GitHub issues have documented problems involving:

- ESM
- CommonJS
- TypeScript
- Cucumber
- path aliases
- module loading

### Challenge

Code can look correct but fail at runtime because the module system is
inconsistent.

### How we overcame it

We explicitly defined:

```text
Node version
+
Module system
+
tsconfig
+
Cucumber/playwright-bdd version
```

and tested the combination together.

We did not let Copilot randomly switch between:

```text
CommonJS
```

and:

```text
NodeNext / ESM
```

just to resolve an individual error.

---

# 26. `cucumber.js` Configuration Did Not Map 1:1

Legacy framework:

```text
cucumber.properties
cucumber.js
Runner
JUnit/TestNG configuration
```

The target `playwright-bdd` configuration is more closely integrated
with Playwright.

Example:

```typescript
const testDir = defineBddConfig({
  features: "features/**/*.feature",
  steps: "steps/**/*.ts",
});
```

and Playwright uses the generated test directory.

### Challenge

Blindly copying Cucumber configuration results in unsupported or
duplicated configuration.

### How we overcame it

We reviewed every configuration item and classified it:

```text
Cucumber option
       ↓
Playwright-BDD equivalent
       OR
Playwright equivalent
       OR
No longer required
```

Migration prompt:

> Analyze every Cucumber configuration option. Determine whether it has
> a Playwright-BDD equivalent, a Playwright equivalent, or should be
> removed. Do not blindly migrate unsupported options.

---

# 27. Cucumber Tags

Existing:

```gherkin
@smoke
Scenario: Login
```

Old execution might be:

```bash
mvn test -Dcucumber.filter.tags="@smoke"
```

### Challenge

Tag execution must be mapped to the new BDD/Playwright execution model.

### How we overcame it

We standardized tags:

```text
@smoke
@regression
@sanity
@api
@ui
@critical
```

and defined how each tag is filtered in the new runner.

---

# 28. Scenario Outlines and DataTables

Existing:

```gherkin
Scenario Outline: Login
  When I login using "<username>" and "<password>"

Examples:
| username | password |
| user1    | pass1    |
| user2    | pass2    |
```

### Challenge

The step definitions need to be converted to TypeScript and correctly
typed.

DataTables similarly require appropriate TypeScript handling.

### How we overcame it

We preserved the business-readable Gherkin wherever possible and
migrated the implementation layer underneath it.

The goal was:

```text
Existing business specification
        ↓
same Gherkin
        ↓
new TypeScript implementation
```

rather than rewriting business scenarios unnecessarily.

---

# 29. Reporting

Legacy framework may contain:

```text
Cucumber HTML
Extent Reports
Allure
Jenkins
Screenshots
Videos
Custom dashboards
```

Playwright provides native reporting capabilities, including HTML
reporting and trace artifacts.

### Challenge

Rebuilding the entire custom reporting system immediately increases
migration risk.

### How we overcame it

We migrated reporting incrementally:

```text
Phase 1
BDD result

Phase 2
Playwright HTML report

Phase 3
Trace + screenshot + video

Phase 4
Custom management report
```

The objective was to preserve existing visibility while progressively
using native Playwright capabilities.

---

# 30. Selenium Grid → Playwright Workers/Sharding

Old:

```text
Selenium Grid
 ├── Node 1
 ├── Node 2
 ├── Node 3
 └── Node 4
```

New:

```text
Playwright
 ├── Worker 1
 ├── Worker 2
 ├── Worker 3
 └── Worker 4
```

and potentially:

```text
CI Job 1 → Shard 1
CI Job 2 → Shard 2
CI Job 3 → Shard 3
```

### Challenge

Simply turning on maximum parallelism can expose:

- data collisions
- environment limits
- account conflicts
- API rate limits
- CPU/memory limitations
- test flakiness

### How we overcame it

Increase parallelism gradually:

```text
1 worker
 ↓
2 workers
 ↓
4 workers
 ↓
8 workers
```

At every stage validate:

```text
Data isolation
Environment stability
Account conflicts
API limits
CPU/memory
Flakiness
```

---

# 31. Custom Utilities Became Technical Debt

Legacy Selenium framework may have:

```text
BrowserUtils
WaitUtils
ElementUtils
JavaScriptUtils
WindowUtils
FrameUtils
AlertUtils
ScreenshotUtils
```

### Challenge

A simple migration might produce:

```text
PlaywrightBrowserUtils
PlaywrightWaitUtils
PlaywrightElementUtils
...
```

This preserves unnecessary Selenium abstractions.

### How we overcame it

For every utility we asked:

> Does Playwright already provide this?

For example:

```text
WaitUtils.waitForElement()
```

may no longer be necessary.

Similarly:

```text
ElementUtils.click()
```

can often become:

```typescript
locator.click();
```

### Principle

> **Remove abstractions that existed only because Selenium required
> them.**

---

# 32. BaseTest Inheritance → Composition

Legacy Java may have:

```text
BaseTest
   ↓
LoginTest
   ↓
OrderTest
   ↓
PaymentTest
```

or:

```text
BaseStepDefinition
   ↓
All step definitions
```

### Challenge

Reproducing a large inheritance hierarchy in TypeScript creates
unnecessary coupling.

### How we overcame it

We moved toward:

```text
Fixtures
+
Composition
+
Page Objects
+
Business Workflows
```

instead of large inheritance trees.

---

# 33. Page Objects Became Too Large

A migrated Page Object can easily become:

```text
CustomerPage.ts
 ├── login
 ├── createCustomer
 ├── editCustomer
 ├── deleteCustomer
 ├── searchCustomer
 ├── approveCustomer
 ├── downloadCustomer
 └── ...
```

### How we overcame it

We introduced page/component separation:

```text
CustomerPage
 ├── CustomerSearchComponent
 ├── CustomerDetailsComponent
 ├── CustomerAddressComponent
 └── CustomerActionsComponent
```

and business workflows:

```text
CreateCustomerWorkflow
UpdateCustomerWorkflow
ApproveCustomerWorkflow
```

This separates UI mechanics from business actions.

---

# 34. Environment Management

Legacy:

```properties
environment=qa
browser=chrome
username=...
```

with environment-specific property files.

Target:

```text
.env
.env.qa
.env.uat
```

or another centralized configuration mechanism.

### Challenge

Copilot may put environment-specific values directly into test files.

### How we overcame it

We created a centralized configuration model:

```typescript
interface TestConfig {
  baseUrl: string;
  apiUrl: string;
  username: string;
  timeout: number;
}
```

Tests should consume configuration without knowing how it is loaded.

### Principle

> **No test should know where environment configuration comes from.**

Secrets must not be committed to Git.

---

# 35. API + UI Integration

Many Selenium Java BDD frameworks use API tools such as RestAssured for
test-data setup.

Example:

```text
API → create customer
     ↓
UI → verify customer
```

### Challenge

The migration should not turn API setup into UI automation.

### How we overcame it

We separated:

```text
BDD
 ↓
Workflow
 ├── UI → Playwright Page
 └── API → API Client
```

Playwright also provides API request capabilities that can be used
alongside UI tests.

This makes test setup much faster.

---

# 36. Database Utilities

Legacy:

```text
JDBC
 ↓
DBUtils
 ↓
SQL
```

### Challenge

If SQL is placed directly inside step definitions, the new framework
becomes tightly coupled to the database.

### How we overcame it

We introduced a service/repository layer:

```text
Step
 ↓
Data Service
 ↓
DB Client
```

For example:

```typescript
const customer = await customerRepository.findById(id);
```

The step does not need to know whether the data source is:

```text
PostgreSQL
Oracle
SQL Server
REST API
```

---

# 37. Screenshots / Video / Trace

Legacy Selenium frameworks often have:

```text
@After
 ↓
if failure
 ↓
takeScreenshot()
 ↓
save path
 ↓
attach to report
```

### Challenge

Rebuilding all artifact handling manually is unnecessary.

### How we overcame it

We used Playwright-native artifacts wherever possible:

```text
Failure
 ↓
Screenshot
 ↓
Trace
 ↓
Video where required
 ↓
HTML report
```

The trace viewer is especially useful because it provides a detailed
timeline of the test execution.

---

# 38. Debugging Strategy Changed

Selenium debugging often relies heavily on:

```text
Breakpoint
 ↓
Inspect WebDriver
 ↓
Browser
 ↓
Logs
```

Playwright provides richer debugging and tracing.

### New debugging strategy

```text
Local
 ↓
headed mode + debugger

CI
 ↓
trace on retry

Failure
 ↓
HTML report
 ↓
trace
 ↓
DOM snapshot
 ↓
network
```

This gives much more information for diagnosing CI-only failures.

---

# 39. Locator Migration Exposed Application Testability Problems

Example:

```html
<button class="btn x92_abc_123"></button>
```

Legacy Selenium:

```xpath
//button[contains(@class,'x92_abc')]
```

### Challenge

Blindly migrating this XPath keeps the locator fragile.

### How we overcame it

Where possible, we introduced stable selectors:

```html
<button data-testid="submit-order"></button>
```

and:

```typescript
page.getByTestId("submit-order");
```

### Important lesson

The migration can also be an opportunity to improve **application
testability**.

---

# 40. Flaky Tests Did Not Automatically Disappear

A common assumption is:

> "Playwright has auto-waiting, therefore all flaky tests disappear."

That is not true.

Playwright synchronization can solve some timing issues, but not:

```text
Shared test data
Race conditions
Backend instability
Application defects
External dependencies
Environment instability
Time-dependent business rules
```

### How we overcame it

We classified flaky failures:

```text
Flaky because of Selenium wait
       ↓
Playwright auto-wait

Flaky because of shared data
       ↓
Unique test data

Flaky because of environment
       ↓
Environment fix

Flaky because of external dependency
       ↓
Mock/stub/API strategy

Flaky because of application defect
       ↓
Real defect
```

---

# 41. Existing Selenium Tests Contained Technical Debt

Suppose the Selenium suite contains:

```text
1,500 scenarios
```

It is tempting to do:

```text
1,500 Selenium tests
        ↓
1,500 Playwright tests
```

### Challenge

The existing suite may contain:

```text
Duplicate tests
Obsolete tests
Flaky tests
Environment-dependent tests
Shared-data tests
Tests covering changed functionality
```

### How we overcame it

We created a migration inventory:

```text
Feature
Scenario
Current status
Business criticality
Selenium dependencies
External dependencies
Migration status
Validation status
```

Then categorized scenarios:

```text
Migrate
Refactor
Retire
Needs business confirmation
```

This prevented the migration from simply transferring technical debt to
the new framework.

---

# 42. Migration Baseline

Before migration, establish metrics:

```text
Total tests
Pass %
Failure %
Flaky %
Average execution time
CI duration
Browser coverage
Smoke duration
Regression duration
```

Example:

```text
Selenium baseline

Total scenarios:      1,250
Smoke:                  110
Regression:           1,140
Average CI duration:    95 min
Flaky scenarios:         8%
```

After migration:

```text
Playwright

Total scenarios:      1,250
Average CI duration:    32 min
Flaky scenarios:         2%
```

The numbers above are illustrative; for an actual project, use measured
values.

### Why baseline matters

Without a baseline, management cannot determine whether the migration
delivered measurable value.

---

# 43. We Did Not Migrate Everything in One Branch

For a large enterprise framework, use a strangler-style migration.

```text
Existing Selenium
       │
       ├── Existing regression
       │
       └── New Playwright
              │
              ├── Smoke
              ├── New features
              └── Migrated regression
```

Run both temporarily.

This gives the team a fallback and allows functional comparison before
Selenium is decommissioned.

---

# 44. Pilot Migration

Do not migrate hundreds or thousands of tests immediately.

Start with approximately:

```text
10–20 representative scenarios
```

The pilot should contain different complexities:

```text
Simple login
Dynamic UI
iframe
Popup
File upload
Download
API + UI
Data-driven
Multi-user
Negative scenario
```

### Why

A simple login test doesn't validate the target architecture.

The pilot should deliberately contain the difficult patterns.

---

# 45. Migrate by Capability, Not Just File-by-File

Bad approach:

```text
LoginTest.java
 ↓
LoginTest.ts

OrderTest.java
 ↓
OrderTest.ts
```

Better:

```text
Capability 1
Authentication

Capability 2
Navigation

Capability 3
Customer

Capability 4
Order

Capability 5
Payment
```

Within each capability:

```text
Feature
 ↓
Steps
 ↓
Workflow
 ↓
Page/component
 ↓
Utilities
```

This creates a coherent framework rather than a collection of translated
files.

---

# 46. Target Architecture

For a serious enterprise Selenium Java → Playwright TypeScript BDD
migration, a suitable target architecture is:

```text
                    ┌──────────────────┐
                    │   Gherkin        │
                    │   Feature Files  │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │   BDD Layer      │
                    │ playwright-bdd   │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │ Step Definitions │
                    └────────┬─────────┘
                             │
                             ▼
                 ┌────────────────────────┐
                 │ Business Workflows     │
                 │ / Tasks / Services      │
                 └───────────┬────────────┘
                             │
                  ┌──────────┴──────────┐
                  ▼                     ▼
        ┌──────────────────┐   ┌──────────────────┐
        │ Page Components  │   │ API/Data Services│
        │ Page Objects     │   │                  │
        └────────┬─────────┘   └────────┬─────────┘
                 │                      │
                 ▼                      ▼
        ┌──────────────────┐   ┌──────────────────┐
        │ Playwright       │   │ API / DB         │
        │ BrowserContext   │   │                  │
        │ Page             │   │                  │
        └──────────────────┘   └──────────────────┘
```

Cross-cutting:

```text
             ┌─────────────────────┐
             │ Configuration       │
             ├─────────────────────┤
             │ Test Data           │
             │ Logging             │
             │ Reporting           │
             │ Tracing             │
             │ Secrets             │
             │ CI/CD               │
             └─────────────────────┘
```

---

# 47. The 10 Main Challenges to Present in an Interview

If asked:

> **"What challenges did you face while migrating Selenium Java BDD to
> Playwright TypeScript BDD using GitHub Copilot?"**

A concise and strong answer is:

---

\# Challenge How we overcame it

---

1 Copilot translated code Defined migration
but did not architecture and Copilot
automatically understand instructions before
target architecture conversion

2 Synchronous Java → async Converted the call chain
TypeScript to `async/await` and
enabled strict
TypeScript

3 WebDriver/ThreadLocal Replaced with
lifecycle Browser/Context/Page and
fixtures

4 Explicit waits and Replaced with Playwright
`Thread.sleep()` synchronization and
web-first assertions

5 XPath-heavy Selenium Introduced
locators role/label/test-id based
locators

6 Selenium Redesigned Page Objects
PageFactory/Base classes using composition

7 Cucumber hooks and Mapped hooks by
parallel lifecycle scenario/worker/global
purpose

8 Shared test data Introduced
isolated/unique data for
parallel execution

9 Cucumber/Extent/Allure Integrated Playwright
reporting reporting and artifacts
incrementally

10 Selenium Grid/CI Migrated to Playwright
execution workers/sharding and
explicit browser
installation

---

---

# 48. Recommended AI-Assisted Migration Workflow

The overall process can be represented as:

```text
              Existing Selenium Java
                       │
                       ▼
              ┌─────────────────┐
              │ Framework        │
              │ Assessment       │
              └────────┬────────┘
                       │
                       ▼
              Architecture Rules
                       │
                       ▼
              ┌─────────────────┐
              │ GitHub Copilot  │
              │ Migration Agent │
              └────────┬────────┘
                       │
             ┌─────────┴─────────┐
             ▼                   ▼
       Code Migration       Architecture
                             Validation
             │                   │
             └─────────┬─────────┘
                       ▼
                 TypeScript
                 Compilation
                       │
                       ▼
                    bddgen
                       │
                       ▼
                 Test Execution
                       │
                       ▼
             Selenium vs Playwright
                Functional Check
                       │
                       ▼
                 Code Review
                       │
                       ▼
               Merge / CI Pipeline
```

---

# 49. Make GitHub Copilot Part of the Migration Architecture

Because Copilot is the migration engine, it should receive explicit
instructions rather than being prompted independently for every file.

Recommended structure:

```text
.github/
   copilot-instructions.md

migration/
   migration-rules.md
   selenium-playwright-mapping.md
   migration-checklist.md
```

### `copilot-instructions.md` should define rules such as

```text
Target:
Playwright + TypeScript + BDD

Do not:
- recreate WebDriverManager
- recreate ThreadLocal<WebDriver>
- recreate PageFactory
- convert Thread.sleep() to waitForTimeout()
- blindly preserve XPath
- use global/static Page objects
- use any to bypass TypeScript errors
- install dependencies blindly to fix errors
- change Gherkin business behavior unnecessarily

Use:
- Playwright fixtures
- BrowserContext isolation
- Playwright Locator API
- async/await
- web-first assertions
- API-based test data setup where appropriate
- centralized configuration
- Playwright trace/reporting
- reusable page components
- business workflows
```

---

# 50. AI + Human Validation Model

The strongest model is:

```text
                GitHub Copilot
                      │
                      ▼
              Generate migration
                      │
                      ▼
              TypeScript compile
                      │
                      ▼
                 BDD generation
                      │
                      ▼
                Test execution
                      │
                      ▼
           Functional comparison
                      │
                      ▼
                Code review
                      │
                      ▼
                 CI validation
                      │
                      ▼
                   Merge
```

The important point is that Copilot is **not the final authority**.

---

# 51. Three Levels of Migration Validation

Every migrated test should pass three levels:

### Level 1 --- Code correctness

```text
Does it compile?
```

### Level 2 --- Execution correctness

```text
Does the Playwright test execute?
```

### Level 3 --- Business correctness

```text
Does it still validate the same business requirement?
```

A test can pass Levels 1 and 2 while failing Level 3.

This is why functional validation is mandatory.

---

# 52. Final Migration Message for Management

A strong management-level explanation is:

> **"We used GitHub Copilot to accelerate the Selenium Java to
> Playwright TypeScript BDD migration. Copilot handled a significant
> portion of the repetitive code conversion, but we did not treat
> generated code as production-ready automatically. We established a
> target architecture and migration rules, validated TypeScript
> compilation and BDD generation, refactored Selenium-specific
> synchronization and driver-management patterns, introduced Playwright
> fixtures and browser-context isolation, addressed parallel test-data
> issues, and validated migrated scenarios against the existing
> functional behavior. This allowed us to use AI for migration speed
> while maintaining architectural and functional quality."**

---

# 53. Key Takeaway

The migration should be described as:

```text
Selenium Java
      +
Existing BDD framework
      +
Existing technical debt
      │
      ▼
Architecture Assessment
      │
      ▼
Migration Rules
      │
      ▼
GitHub Copilot
      │
      ▼
AI-Assisted Code Conversion
      │
      ▼
Human + Automated Validation
      │
      ▼
Playwright TypeScript BDD
```

The **real challenge was not converting Java syntax to TypeScript**.

The real challenges were:

1.  Making Copilot follow the target architecture.
2.  Replacing Selenium-specific patterns instead of reproducing them.
3.  Handling Playwright's asynchronous execution model.
4.  Redesigning browser/context/fixture lifecycle.
5.  Replacing explicit waits with Playwright's synchronization model.
6.  Improving locator strategy.
7.  Handling Cucumber/Playwright-BDD lifecycle and configuration
    differences.
8.  Making the suite safe for parallel execution.
9.  Maintaining reporting and CI/CD.
10. Proving that AI-generated migrated tests still perform the intended
    functional validation.

---

# Sources / Real GitHub and Playwright References

The research for this document included real GitHub repository/issues
and Playwright documentation covering:

- `playwright-bdd` architecture and documentation
- Cucumber dependency and runner changes
- ESM/CommonJS issues
- custom fixture typing
- hooks and parallel execution
- reusable/common steps
- configuration
- reporting
- Playwright BrowserContext isolation
- Playwright fixtures
- Playwright authentication
- Playwright parallelism/sharding
- Playwright actionability and auto-waiting
- Playwright tracing and reporting
- GitHub Copilot's responsible-use guidance

The key repositories/issues reviewed included the `playwright-bdd`
GitHub repository and issues concerning Cucumber integration, ESM,
fixtures, hooks, parallel execution, configuration, reusable steps and
reporting, as well as Cucumber.js TypeScript/ESM issues.
