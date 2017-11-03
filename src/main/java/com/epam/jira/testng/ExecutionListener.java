package com.epam.jira.testng;

import com.epam.jira.entity.Issue;
import com.epam.jira.entity.Issues;
import com.epam.jira.entity.Parameter;
import com.epam.jira.entity.TestResult;
import com.epam.jira.util.FileUtils;
import com.epam.jira.util.JiraInfoProvider;
import com.epam.jira.util.Screenshoter;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import java.util.*;
import java.util.stream.Collectors;

import static com.epam.jira.testng.TestNGUtils.getTestGroupsDependencies;
import static com.epam.jira.testng.TestNGUtils.getTestMethodDependencies;

public class ExecutionListener extends TestListenerAdapter {

    private List<Issue> issues = new ArrayList<>();
    private Map<String, Issue> failedMethods = new HashMap<>();
    private Map<String, List<Issue>> failedGroups = new HashMap<>();

    @Override
    public void onTestSuccess(ITestResult result) {
        super.onTestSuccess(result);
        String key = TestNGUtils.getTestJIRATestKey(result);
        if (key != null) {
            issues.add(new Issue(key, TestResult.PASSED, TestNGUtils.getTimeAsString(result)));
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        super.onTestFailure(result);

        String key = TestNGUtils.getTestJIRATestKey(result);
        Issue issue = null;
        List<String> attachments = new ArrayList<>();

        if (key != null) {
            String screenshot = null;
            String summary;
            if (Screenshoter.isInitialized()) {
                screenshot = Screenshoter.takeScreenshot();
            }

            issue = new Issue(key, TestResult.FAILED, TestNGUtils.getTimeAsString(result));

            // Save failure message and/or trace
            Throwable throwable = result.getThrowable();
            if (throwable instanceof AssertionError) {
                summary = "Assertion failed: " + throwable.getMessage();
            } else {
                String filePath = "stacktrace-" + System.nanoTime()  + ".txt";
                FileUtils.writeStackTrace(throwable, filePath);
                attachments.add(FileUtils.getAttachmentsDir() + filePath);
                summary = "Failed due to: " + throwable.getClass().getName() + ": " + throwable.getMessage()
                        + ". Full stack trace attached as " + filePath;
            }

            // Save screenshot if possible
            if (screenshot != null) {
                attachments.add(FileUtils.getAttachmentsDir() + screenshot);
                summary += ". Screenshot attached as " + screenshot;
            }
            issue.setSummary(summary);
            if (!attachments.isEmpty())
                issue.setAttachments(attachments);
            issues.add(issue);
        }
        saveMethodAndGroupsInFailed(result, issue);


    }

    private void saveMethodAndGroupsInFailed(ITestResult result, Issue issue) {
        String [] groups = TestNGUtils.getMethodGroups(result);
        String methodName = TestNGUtils.getMethodName(result);

        failedMethods.put(methodName, issue);
        for (String group : groups) {
            List<Issue> issues = failedGroups.get(group);
            if (issues != null) {
                issues.add(issue);
            } else {
                failedGroups.put(group, Arrays.asList(issue));
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        super.onTestSkipped(result);
        String key = TestNGUtils.getTestJIRATestKey(result);
        if (key != null) {
            Issue testCase = new Issue(key, TestResult.BLOCKED);
            StringBuilder blockReasons = new StringBuilder();
            String methodName = TestNGUtils.getMethodName(result);
            Integer dependencyCounter = 0;

            blockReasons.append("Test method ").append(methodName).append(" ").append(testCase).append(" depends on");
            addMethodDependencies(dependencyCounter, blockReasons, getTestMethodDependencies(result));
            addGroupDependencies(dependencyCounter, blockReasons, getTestGroupsDependencies(result));

            testCase.setSummary(blockReasons.toString());
            issues.add(testCase);
        }
    }

    private void addMethodDependencies(Integer dependencyCounter, StringBuilder builder, String [] methods) {
        for (String method : methods) {
            if (failedMethods.containsKey(method)) {
                if (dependencyCounter++ > 0) builder.append(",");
                builder.append(" method ").append(method);
                Issue failedCase = failedMethods.get(method);
                if (failedCase != null) builder.append(" ").append(failedCase);
            }
        }
    }

    private void addGroupDependencies(Integer dependencyCounter, StringBuilder builder, String [] groups) {
        for (String group : groups) {
            if (failedGroups.containsKey(group)) {
                if (dependencyCounter++ > 0) builder.append(",");
                builder.append(" group ").append(group);
                List<Issue> groupTestCases = failedGroups.get(group);
                if (groupTestCases != null && !groupTestCases.isEmpty())
                    builder.append(" with next failed cases: {").append(System.lineSeparator())
                            .append(groupTestCases.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                            .append("}");
            }
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        super.onFinish(context);
        for(ITestNGMethod method : context.getExcludedMethods()) {
            String key = TestNGUtils.getTestJIRATestKey(method);
            if (key != null) {
                issues.add(new Issue(key, TestResult.UNTESTED));
            }
        }

        for(Issue issue : issues) {
            List<String> attachments = JiraInfoProvider.getIssueAttachments(issue.getIssueKey());
            List<Parameter> parameters = JiraInfoProvider.getIssueParameters(issue.getIssueKey());
            if (attachments != null) {
                if (issue.getAttachments() != null)
                    issue.getAttachments().addAll(attachments);
                else
                    issue.setAttachments(attachments);
            }
            if (parameters != null) {
                issue.setParameters(parameters);
            }
        }

        if (!issues.isEmpty())
            FileUtils.writeXml(new Issues(issues), "tm-testng.xml");
    }




}
