# Java TestNG API Automation Framework

A Java 25, Maven, REST Assured, and TestNG API test framework with Allure and ExtentReports. The sample suite uses a local WireMock API by default, so CRUD and negative scenario examples run deterministically without an external service.

## Requirements

- JDK 25
- Maven 3.9+

## Run the suite

```shell
mvn clean test
```

The TestNG suite runs methods and parallel data-provider cases with five workers. It writes the Extent report to `target/extent-reports/extent-report.html` and Allure results to `target/allure-results`.

Generate and view the Allure report with:

```shell
mvn allure:report
mvn allure:serve
```

## Configuration

Edit `src/test/resources/config.properties`, or override values with JVM system properties or environment variables. System properties take precedence, followed by environment variables and then the properties file.

| Property | Environment variable | Purpose |
|---|---|---|
| `api.base.uri` | `API_BASE_URI` | API URI used when the local test server is disabled |
| `api.environment` | `API_ENVIRONMENT` | Environment label |
| `api.timeout.millis` | `API_TIMEOUT_MILLIS` | HTTP connection and socket timeout |
| `api.test.server.enabled` | `API_TEST_SERVER_ENABLED` | Starts deterministic local fixtures for sample tests |

Set `api.test.server.enabled=false` (or `API_TEST_SERVER_ENABLED=false`) to run against the configured base URI. The sample test endpoints expect the response contract represented by the local WireMock fixtures.
