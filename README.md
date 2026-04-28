## In this project, PostgreSQL is not installed directly on your machine, but runs inside a Docker container.You can find its configuration in the infra/docker-compose-web.yaml file:

postgres-db:
image: postgres:15.5-alpine
container_name: postgres-db

## This Docker Compose setup downloads the postgres:15.5-alpine image and runs it as a database service.

## Project Architecture
Based on the pom.xml and docker-compose-web.yaml, this project is a test automation framework using Healenium, an AI-powered tool that automatically "heals" broken Selenium locators (e.g., when the UI of a web application changes, tests don't immediately fail but find the next best selector).The architecture consists of two main parts:1. Backend Infrastructure (Docker)This is defined in infra/docker-compose-web.yaml and sets up a localized network (healenium) with 3 containers:•postgres-db: The PostgreSQL database. It stores the history of successful UI locators, test execution results, and DOM element snapshots.

The architecture consists of two main parts:
1. Backend Infrastructure (Docker)

This is defined in infra/docker-compose-web.yaml and sets up a localized network (healenium) with 3 containers:
•postgres-db: The PostgreSQL database. It stores the history of successful UI locators, test execution results, and DOM element snapshots.
•healenium (hlm-backend): A Spring Boot application that acts as the core backend. It connects to the Postgres DB to store/fetch locator data.
•selector-imitator: A machine-learning component that analyzes the DOM and generates the most optimal new CSS/XPath locators when the original ones break.

2. Client / Test Execution (Java)
This is your Java code (auto-healing-solution in pom.xml):

*Healenium-Web Library: Your code depends on com.epam.healenium:healenium-web. It wraps standard Selenium WebDriver commands.
•Flow: When a test runs (using TestNG and Selenium), the Healenium library intercepts findElement calls. If an element is found, it saves its state to the backend. If an element is missing, it talks to the hlm-backend and selector-imitator to find the new element on the page, dynamically heals the test at runtime, and logs it.

## how to execute this project ?
To execute this project, you need to follow a two-step 
process: first, start the Healenium backend infrastructure 
using Docker, and then run the Java test suite using Maven.

## Step 1: Start the Backend Infrastructure
The auto-healing capabilities depend on a backend database 
and machine-learning service. You need Docker Desktop 
running for this.

Open your terminal, navigate to the infra folder of 
your project, and run Docker Compose:

cd infra
docker compose -f docker-compose-web.yaml up -d

(Wait a minute or two for all services: postgres-db, healenium, and selector-imitator to be fully up and running).

## Step 2: Run the Tests
Once the infrastructure is running, you can execute the test 
suite. Since this is a standard Maven project configured with 
TestNG, you can run it from the root directory of the project.

Open a new terminal (or navigate back to the root heal 
folder) and run:
mvn clean test

## What happens when you run it?

1.Maven will compile the code and trigger the maven-surefire-plugin.
2.It will execute the tests defined in src/test/resources/testng.xml (which includes LoginSauceTest and SelfHealingExampleTest).
3.Chrome will open, navigate to saucedemo.com, and attempt to log in using the locators defined in src/test/resources/locator/firstlogin.json.
4.If any UI element's ID or Class has changed (or if it fails to find the primary locator), the code uses the SelfHealingEngine to automatically calculate similarity and find the new element on the screen without failing your test.


## if I dont have docker setup, how to run this ?
You can run this project even without Docker!
Although the pom.xml contains the official healenium-web 
dependency (which requires the Docker backend), your 
project actually includes its own custom-built Java 
implementation of auto-healing called SelfHealingEngine.java.

This custom engine uses Apache OpenNLP to tokenize DOM 
attributes and mathematically calculate Cosine Similarity 
between your broken locator and the actual elements on the 
screen—completely independent of Docker or Postgres!

You can run the tests right now just using Maven.

## How to run it:
1.Open your terminal in the root directory of the project (C:/Users/Vivek/Downloads/heal).
2.Run the Maven test command:
mvn clean test

## How it works without Docker:
If you look closely at the LocatorManager.java 
and SelfHealingEngine.java in your code:

1.The test tries to find an element using the primary locator loaded from firstlogin.json (e.g., By.id("user-name")).
2.If the locator fails, the LocatorManager catches the exception and falls back to your custom SelfHealingEngine.findBestMatch().
3.The custom engine grabs all visible elements on the screen.
4.It reads the ID, Name, Class, Aria-label, and Text of every element and compares them to your broken locator name using an NLP similarity score algorithm.
5.It returns the element with the highest match (above a 0.5 threshold) and continues the test!

Because of this brilliant fallback mechanism built directly into your Java code, the tests will successfully execute and self-heal even if the external Docker infrastructure is offline or Database is totally offline.


   




