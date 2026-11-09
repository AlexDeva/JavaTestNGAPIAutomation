package com.example.apiautomation.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

import java.nio.file.Path;

/** Owns the shared Extent report and the per-test context used by parallel TestNG workers. */
public final class ExtentReportManager {
    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();
    private static ExtentReports extentReports;

    private ExtentReportManager() {
    }

    /** Creates the shared report once and initializes its output file. */
    public static synchronized void initialize() {
        if (extentReports == null) {
            Path reportPath = Path.of("target", "extent-reports", "extent-report.html");
            reportPath.toFile().getParentFile().mkdirs();
            ExtentSparkReporter reporter = new ExtentSparkReporter(reportPath.toString());
            reporter.config().setDocumentTitle("API Automation Results");
            reporter.config().setReportName("API Automation Test Execution");
            extentReports = new ExtentReports();
            extentReports.attachReporter(reporter);
        }
    }

    /** Creates and binds a test node to the calling TestNG worker thread. */
    public static void startTest(String testName) {
        initialize();
        synchronized (ExtentReportManager.class) {
            CURRENT_TEST.set(extentReports.createTest(testName));
        }
    }

    /** Logs an informational message to the current thread's test node when one exists. */
    public static void logInfo(String message) {
        ExtentTest test = CURRENT_TEST.get();
        if (test != null) {
            test.info(message);
        }
    }

    /** Flushes completed test results to the configured Extent HTML report. */
    public static synchronized void flush() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }

    /** Removes the current worker's test context to prevent it leaking into a later test. */
    public static void removeTest() {
        CURRENT_TEST.remove();
    }
}
