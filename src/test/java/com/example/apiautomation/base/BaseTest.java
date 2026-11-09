package com.example.apiautomation.base;

import com.example.apiautomation.config.ConfigManager;
import com.example.apiautomation.reporting.ExtentReportManager;
import com.example.apiautomation.support.TestApiServer;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

/** Configures the shared HTTP client and manages suite-level test infrastructure. */
public abstract class BaseTest {
    private String previousBaseUri;
    private boolean hadPreviousBaseUri;

    /** Starts optional local fixtures, applies timeouts, and initializes report output before the suite. */
    @BeforeSuite(alwaysRun = true)
    public void setUpSuite() {
        ExtentReportManager.initialize();
        RestAssured.config = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", ConfigManager.getTimeoutMillis())
                        .setParam("http.socket.timeout", ConfigManager.getTimeoutMillis()));

        if (ConfigManager.isTestServerEnabled()) {
            previousBaseUri = System.getProperty("api.base.uri");
            hadPreviousBaseUri = previousBaseUri != null;
            TestApiServer.start();
            System.setProperty("api.base.uri", TestApiServer.getBaseUri());
        }
    }

    /** Logs per-test configuration; HTTP configuration remains immutable while parallel tests are running. */
    @BeforeMethod(alwaysRun = true)
    public void setUpTest() {
        ExtentReportManager.logInfo("Environment: " + ConfigManager.getEnvironment());
    }

    /** Records per-method cleanup; request specifications are local to APIUtils and require no shared reset. */
    @AfterMethod(alwaysRun = true)
    public void tearDownTest() {
        ExtentReportManager.logInfo("Test method cleanup complete");
    }

    /** Stops suite infrastructure and restores a caller-supplied base URI after test execution. */
    @AfterSuite(alwaysRun = true)
    public void tearDownSuite() {
        TestApiServer.stop();
        if (hadPreviousBaseUri) {
            System.setProperty("api.base.uri", previousBaseUri);
        } else if (ConfigManager.isTestServerEnabled()) {
            System.clearProperty("api.base.uri");
        }
        ExtentReportManager.flush();
    }
}
