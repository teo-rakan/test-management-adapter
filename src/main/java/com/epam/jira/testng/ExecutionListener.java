package com.epam.jira.testng;

import com.epam.jira.entity.Issue;
import com.epam.jira.entity.Issues;
import com.epam.jira.entity.Parameter;
import com.epam.jira.entity.TestResult;
import com.epam.jira.util.FileUtils;
import com.epam.jira.util.JiraInfoProvider;
import com.epam.jira.util.Screenshoter;
import org.testng.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ExecutionListener extends TestListenerAdapter {

    private final List<Issue> issues = new ArrayList<>();
    private final SkipAnalyzer skipAnalyzer = new SkipAnalyzer();

    @Override
    public void onConfigurationFailure(ITestResult result) {
        super.onConfigurationSkip(result);
        skipAnalyzer.addFailedConfig(result);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        super.onTestSuccess(result);
        String key = TestNGUtils.getTestJIRATestKey(result);
        if (key != null) {
            Issue issue = new Issue(key, TestResult.PASSED, TestNGUtils.getTimeAsString(result));
            IRetryAnalyzer analyzer = result.getMethod().getRetryAnalyzer();
            if (analyzer instanceof RetryAnalyzer) {
                int attempts = ((RetryAnalyzer) analyzer).getRerunAttempts();
                if (attempts > 0) issue.setSummary("Test was rerun " + attempts + " times.");
            }
            issues.add(issue);
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        super.onTestFailure(result);

        String key = TestNGUtils.getTestJIRATestKey(result);
        Issue issue = null;
        List<String> attachments = new ArrayList<>();

        if (key != null) {
            String screenshot = Screenshoter.isInitialized() ? Screenshoter.takeScreenshot() : null;
            String summary;

            issue = new Issue(key, TestResult.FAILED, TestNGUtils.getTimeAsString(result));

            // Save failure message and/or trace
            Throwable throwable = result.getThrowable();
            if (throwable instanceof AssertionError) {
                summary = "Assertion failed: " + throwable.getMessage();
            } else {
                summary = FileUtils.save(result.getThrowable());
                attachments.add(getAttachmentPath(summary));
            }

            // Save screenshot if possible
            if (screenshot != null) {
                attachments.add(FileUtils.getAttachmentsDir() + screenshot);
                summary += ".\nScreenshot attached as " + screenshot;
            }

            IRetryAnalyzer analyzer = result.getMethod().getRetryAnalyzer();
            if (analyzer != null && analyzer instanceof RetryAnalyzer) {
                int attempts = ((RetryAnalyzer) analyzer).getRerunAttempts();
                if (attempts > 0) {
                    summary += ".\nTest was rerun " + attempts + " times.";
                }
            }

            issue.setSummary(summary);
            if (!attachments.isEmpty())
                issue.setAttachments(attachments);
            issues.add(issue);
        }
        skipAnalyzer.addFailedResult(result, issue);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        super.onTestSkipped(result);
        String key = TestNGUtils.getTestJIRATestKey(result);
        if (key != null) {
            // Check if retry
            IRetryAnalyzer retryAnalyzer = result.getMethod().getRetryAnalyzer();
            if (retryAnalyzer != null && retryAnalyzer instanceof RetryAnalyzer) {
                if (((RetryAnalyzer) retryAnalyzer).wasRerun()) return;
            }

            Issue issue = new Issue(key, TestResult.BLOCKED);

            // Check dependencies
            String dependencies = skipAnalyzer.getReasonIfHaveDependencies(result);
            if (dependencies != null) {
                issue.setSummary(String.format(dependencies, key));
            } else {
                String message = skipAnalyzer.getLastFailedConfig();
                if (message != null){
                    List<String> attachments = new ArrayList<>();
                    attachments.add(getAttachmentPath(message));
                    issue.setSummary("Test method was blocked because of failed config method " + message);
                    issue.setAttachments(attachments);
                }
            }
            issues.add(issue);
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        super.onFinish(context);
        for (ITestNGMethod method : context.getExcludedMethods()) {
            String key = TestNGUtils.getTestJIRATestKey(method);
            if (key != null) issues.add(new Issue(key, TestResult.UNTESTED));
        }

        for (Issue issue : issues) {
            List<String> attachments = JiraInfoProvider.getIssueAttachments(issue.getIssueKey());
            List<Parameter> parameters = JiraInfoProvider.getIssueParameters(issue.getIssueKey());
            if (attachments != null) {
                if (issue.getAttachments() != null)
                    issue.getAttachments().addAll(attachments);
                else
                    issue.setAttachments(attachments);
            }
            if (parameters != null) issue.setParameters(parameters);
        }

        if (!issues.isEmpty()) FileUtils.writeXml(new Issues(issues), "tm-testng.xml");
    }

    // Auxiliary methods

    private String getAttachmentPath(String message) {
        Pattern pattern = Pattern.compile("stacktrace.\\d{4}-\\d{2}-\\d{2}T.*");
        Matcher matcher = pattern.matcher(message);
        return (matcher.find()) ? FileUtils.getAttachmentsDir() + matcher.group() : null;
    }
}
