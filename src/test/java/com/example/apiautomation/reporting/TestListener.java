package com.example.apiautomation.reporting;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/** Bridges TestNG lifecycle events into the thread-safe Extent report. */
public class TestListener implements ITestListener {
    @Override
    public void onTestStart(ITestResult result) {
        // TestNG may invoke callbacks on parallel workers; the manager binds each test node with ThreadLocal.
        ExtentReportManager.startTest(result.getMethod().getQualifiedName());
        ExtentReportManager.logInfo("Test started");
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentReportManager.logInfo("Test passed");
        ExtentReportManager.removeTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentReportManager.logInfo("Test failed: " + result.getThrowable());
        ExtentReportManager.removeTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentReportManager.logInfo("Test skipped: " + result.getThrowable());
        ExtentReportManager.removeTest();
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentReportManager.flush();
    }
}
