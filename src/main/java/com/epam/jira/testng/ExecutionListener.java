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

import java.lang.annotation.Annotation;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.epam.jira.testng.TestNGUtils.*;

public class ExecutionListener extends TestListenerAdapter {

    private final String STACK_TRACE_FILE = "stacktrace_%s.txt";
    private final List<Issue> issues = new ArrayList<>();
    private final Map<String, Issue> failedMethods = new HashMap<>();
    private final Map<String, List<Issue>> failedGroups = new HashMap<>();
    private final List<String> failedConfigs = new ArrayList<>();

    @Override
    public void onConfigurationFailure(ITestResult result) {
        Annotation[] annotations = result.getMethod().getConstructorOrMethod().getMethod().getDeclaredAnnotations();
        String annotationInfo = (annotations != null && annotations.length > 0)
                ? " annotated with @" + Arrays.stream(annotations).map(a -> a.annotationType().getSimpleName()).collect(Collectors.joining(", @"))
                : "";
        failedConfigs.add(TestNGUtils.getFullMethodName(result) + annotationInfo);
    }

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
            String screenshot = Screenshoter.isInitialized() ? Screenshoter.takeScreenshot() : null;
            String summary;

            issue = new Issue(key, TestResult.FAILED, TestNGUtils.getTimeAsString(result));

            // Save failure message and/or trace
            Throwable throwable = result.getThrowable();
            if (throwable instanceof AssertionError) {
                summary = "Assertion failed: " + throwable.getMessage();
            } else {
                String filePath = String.format(STACK_TRACE_FILE, LocalDateTime.now().toString().replace(":", "-"));
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

    @Override
    public void onTestSkipped(ITestResult result) {
        super.onTestSkipped(result);
        String key = TestNGUtils.getTestJIRATestKey(result);
        if (key != null) {
            Issue testCase = new Issue(key, TestResult.BLOCKED);
            StringBuilder blockReasons = new StringBuilder();
            String methodName = TestNGUtils.getFullMethodName(result);
            Integer dependencyCounter = 0;

            blockReasons.append("Test method ").append(methodName).append(" (").append(testCase.getIssueKey())
                    .append(") depends on");
            dependencyCounter = addMethodDependencies(dependencyCounter, blockReasons, result);
            dependencyCounter = addGroupDependencies(dependencyCounter, blockReasons, result);

            if (dependencyCounter > 0) {
                testCase.setSummary(blockReasons.toString());
            } else if (!failedConfigs.isEmpty()){
                testCase.setSummary("Test method was blocked because of failed config method " + failedConfigs.get(failedConfigs.size() - 1));
            }
            issues.add(testCase);
        }
    }

    //todo move
    private int addMethodDependencies(int dependencyCounter, StringBuilder builder, ITestResult result) {
        String[] methods = getTestMethodDependencies(result);
        String testClassName = getTestClassName(result);
        for (String method : methods) {
            String fullMethodName = testClassName + "." + method;
            if (failedMethods.containsKey(fullMethodName)) {
                if (dependencyCounter++ > 0) builder.append(",");
                builder.append(" method ").append(fullMethodName);
                Issue failedCase = failedMethods.get(fullMethodName);
                if (failedCase != null) builder.append(" (").append(failedCase.getIssueKey()).append(")");
            }
        }
        return dependencyCounter;
    }

    private int addGroupDependencies(int dependencyCounter, StringBuilder builder, ITestResult result) {
        String[] groups = getTestGroupsDependencies(result);
        for (String group : groups) {
            if (failedGroups.containsKey(group)) {
                if (dependencyCounter++ > 0) builder.append(",");
                builder.append(" group \"").append(group).append("\"");
                List<Issue> groupTestCases = failedGroups.get(group);
                if (groupTestCases != null && !groupTestCases.isEmpty())
                    builder.append(" with next failed cases: (")
                            .append(groupTestCases.stream().map(Issue::getIssueKey).collect(Collectors.joining(", ")))
                            .append(")");
            }
        }
        return dependencyCounter;
    }

    @Override
    public void onFinish(ITestContext context) {
        super.onFinish(context);
        for (ITestNGMethod method : context.getExcludedMethods()) {
            String key = TestNGUtils.getTestJIRATestKey(method);
            if (key != null) {
                issues.add(new Issue(key, TestResult.UNTESTED));
            }
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
            if (parameters != null) {
                issue.setParameters(parameters);
            }
        }

        if (!issues.isEmpty())
            FileUtils.writeXml(new Issues(issues), "tm-testng.xml");
    }


}
