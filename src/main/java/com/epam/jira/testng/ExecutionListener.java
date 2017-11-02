package com.epam.jira.testng;

import com.epam.jira.core.JiraTestCase;
import com.epam.jira.core.Screenshoter;
import com.epam.jira.core.TestResult;
import com.epam.jira.util.FileUtils;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import java.util.*;
import java.util.stream.Collectors;

import static com.epam.jira.testng.TestNGUtils.getTestGroupsDependencies;
import static com.epam.jira.testng.TestNGUtils.getTestMethodDependencies;

public class ExecutionListener extends TestListenerAdapter {

    private List<JiraTestCase> tests = new ArrayList<>();
    private Map<String, JiraTestCase> failedMethods = new HashMap<>();
    private Map<String, List<JiraTestCase>> failedGroups = new HashMap<>();
    private final String TARGET_DIR = "/target";

    @Override
    public void onTestSuccess(ITestResult iTestResult) {
        super.onTestSuccess(iTestResult);
        String key = TestNGUtils.getTestJIRATestKey(iTestResult);
        if (key != null) {
            tests.add(new JiraTestCase(key, TestResult.PASSED));
        }
    }

    @Override
    public void onTestFailure(ITestResult iTestResult) {
        super.onTestFailure(iTestResult);

        String key = TestNGUtils.getTestJIRATestKey(iTestResult);
        JiraTestCase testCase = null;

        if (key != null) {
            String screenshot = null;
            String comment;
            if (Screenshoter.isInitialized()) {
                screenshot = Screenshoter.takeScreenshot(TARGET_DIR);
            }

            testCase = new JiraTestCase(key, TestResult.FAILED);

            // Save failure message and/or trace
            Throwable throwable = iTestResult.getThrowable();
            if (throwable instanceof AssertionError) {
                comment = "Assertion failed: " + throwable.getMessage();
            } else {
                String filePath = TARGET_DIR + "/stacktrace-" + key  + ".txt";
                FileUtils.writeStackTrace(throwable, filePath);
                testCase.addFilePath(filePath);
                comment = "Failed due to: " + throwable.getClass().getName() + ": " + throwable.getMessage()
                        + " . Full stack trace attached as 'stacktrace-" + key  + ".txt'";
            }

            // Save screenshot if possible
            if (screenshot != null) {
                testCase.addFilePath(TARGET_DIR + screenshot);
                comment += ". Screenshot attached: " + screenshot;
            }
            testCase.addComment(comment);
            tests.add(testCase);
        }
        saveMethodAndGroupsInFailed(iTestResult, testCase);


    }

    private void saveMethodAndGroupsInFailed(ITestResult iTestResult, JiraTestCase testCase) {
        String [] groups = TestNGUtils.getMethodGroups(iTestResult);
        String methodName = TestNGUtils.getMethodName(iTestResult);

        failedMethods.put(methodName, testCase);
        for (String group : groups) {
            List<JiraTestCase> testCases = failedGroups.get(group);
            if (testCases != null) {
                testCases.add(testCase);
            } else {
                failedGroups.put(group, Arrays.asList(testCase));
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult iTestResult) {
        super.onTestSkipped(iTestResult);
        String key = TestNGUtils.getTestJIRATestKey(iTestResult);
        if (key != null) {
            JiraTestCase testCase = new JiraTestCase(key, TestResult.BLOCKED);
            StringBuilder blockReasons = new StringBuilder();
            String methodName = TestNGUtils.getMethodName(iTestResult);
            Integer dependencyCounter = 0;

            blockReasons.append("Test method ").append(methodName).append(" ").append(testCase).append(" depends on");
            addMethodDependencies(dependencyCounter, blockReasons, getTestMethodDependencies(iTestResult));
            addGroupDependencies(dependencyCounter, blockReasons, getTestGroupsDependencies(iTestResult));

            testCase.addComment(blockReasons.toString());
            tests.add(testCase);
        }
    }

    private void addMethodDependencies(Integer dependencyCounter, StringBuilder builder, String [] methods) {
        for (String method : methods) {
            if (failedMethods.containsKey(method)) {
                if (dependencyCounter++ > 0) builder.append(",");
                builder.append(" method ").append(method);
                JiraTestCase failedCase = failedMethods.get(method);
                if (failedCase != null) builder.append(" ").append(failedCase);
            }
        }
    }

    private void addGroupDependencies(Integer dependencyCounter, StringBuilder builder, String [] groups) {
        for (String group : groups) {
            if (failedGroups.containsKey(group)) {
                if (dependencyCounter++ > 0) builder.append(",");
                builder.append(" group ").append(group);
                List<JiraTestCase> groupTestCases = failedGroups.get(group);
                if (groupTestCases != null && !groupTestCases.isEmpty())
                    builder.append(" with next failed cases: {").append(System.lineSeparator())
                            .append(groupTestCases.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                            .append("}");
            }
        }
    }

    @Override
    public void onFinish(ITestContext iTestContext) {
        super.onFinish(iTestContext);
        for(ITestNGMethod method : iTestContext.getExcludedMethods()) {
            String key = TestNGUtils.getTestJIRATestKey(method);
            if (key != null) {
                tests.add(new JiraTestCase(key, TestResult.UNTESTED));
            }
        }
        if (!tests.isEmpty())
            FileUtils.writeXmlFile(tests, TARGET_DIR + "/tm-testng.xml");
    }




}
