package com.epam.jira.testng;

import com.epam.jira.entity.Issue;
import com.epam.jira.entity.Issues;
import com.epam.jira.entity.Parameter;
import com.epam.jira.entity.TestResult;
import com.epam.jira.util.FileUtils;
import com.epam.jira.util.JiraInfoProvider;
import com.epam.jira.util.Screenshoter;
import org.testng.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExecutionListener extends TestListenerAdapter {

    private final List<Issue> issues = new ArrayList<>();
    private final Map<String, Issue> failedMethods = new HashMap<>();
    private final Map<String, List<Issue>> failedGroups = new HashMap<>();
    private final List<String> failedConfigs = new ArrayList<>();

    @Override
    public void onConfigurationFailure(ITestResult result) {
        Annotation[] annotations = result.getMethod().getConstructorOrMethod().getMethod().getDeclaredAnnotations();
        String annotationInfo = annotationsToString(annotations);
        failedConfigs.add(TestNGUtils.getFullMethodName(result) + annotationInfo + ".\n" + save(result.getThrowable()));
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
                summary = save(result.getThrowable());
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
        saveMethodAndGroupsInFailed(result, issue);
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
            Method method = result.getMethod().getConstructorOrMethod().getMethod();
            String dependencies = new SkipAnalyzer().getReasonIfHaveDependencies(method, failedMethods, failedGroups);
            if (dependencies != null) {
                issue.setSummary(String.format(dependencies, key));
            } else if (!failedConfigs.isEmpty()){
                String message = failedConfigs.get(failedConfigs.size() - 1);
                List<String> attachments = new ArrayList<>();
                attachments.add(getAttachmentPath(message));
                issue.setSummary("Test method was blocked because of failed config method " + message);
                issue.setAttachments(attachments);
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

    private String save(Throwable throwable) {
        String message = null;
        if (throwable != null) {
            String filePath = String.format("stacktrace_%s.txt", LocalDateTime.now().toString().replace(":", "-"));
            FileUtils.writeStackTrace(throwable, filePath);
            message = "Failed due to: " + throwable.getClass().getName() + ": " + throwable.getMessage()
                    + ".\nFull stack trace attached as " + filePath;
        }
        return message;
    }

    private String getAttachmentPath(String message) {
        Pattern pattern = Pattern.compile("stacktrace.\\d{4}-\\d{2}-\\d{2}T.*");
        Matcher matcher = pattern.matcher(message);
        return (matcher.find()) ? FileUtils.getAttachmentsDir() + matcher.group() : null;
    }

    private void saveMethodAndGroupsInFailed(ITestResult result, Issue issue) {
        String[] groups = TestNGUtils.getMethodGroups(result);
        String methodName = TestNGUtils.getFullMethodName(result);

        failedMethods.put(methodName, issue);
        for (String group : groups) {
            List<Issue> issues = failedGroups.get(group);
            if (issues != null) {
                issues.add(issue);
            } else {
                List<Issue> failedIssues = new ArrayList<>();
                failedIssues.add(issue);
                failedGroups.put(group, failedIssues);
            }
        }
    }

    private String annotationsToString(Annotation [] annotations) {
        String result = "";
        if (annotations != null && annotations.length > 0)
            result = "annotated with @" + Arrays.stream(annotations).map(a -> a.annotationType().getSimpleName())
                    .collect(Collectors.joining(", @"));
        return result;
    }
}
