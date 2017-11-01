package com.epam.jira.testng;

import com.epam.jira.JIRATestKey;
import com.epam.jira.core.JiraTestCase;
import com.epam.jira.core.TestResult;
import com.epam.jira.util.XMLWriter;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ExecutionListener extends TestListenerAdapter {

    private List<JiraTestCase> tests = new ArrayList<>();
    //private List<JiraTestCase>

    @Override
    public void onTestSuccess(ITestResult iTestResult) {
        super.onTestSuccess(iTestResult);
        String key = getTestJIRATestKey(iTestResult);
        if (key != null) {
            tests.add(new JiraTestCase(key, TestResult.PASSED));
        }
    }

    @Override
    public void onTestFailure(ITestResult iTestResult) {
        super.onTestFailure(iTestResult);
        String key = getTestJIRATestKey(iTestResult);
        if (key != null) {
            Throwable throwable = iTestResult.getThrowable();
            if (throwable instanceof AssertionError) {
                tests.add(new JiraTestCase(key, String.join(System.lineSeparator(), throwable.getMessage())));
            } else {
                //todo create file with stacktrace
                String stackTraceFile = "./target/stacktrace-" + key  + ".txt";
                PrintWriter writer = null;
                try {
                    writer = new PrintWriter(stackTraceFile, "UTF-8");
                } catch (FileNotFoundException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                for (StackTraceElement element : throwable.getStackTrace()){
                    writer.println(element.toString());
                }
                writer.close();

                JiraTestCase testCase = new JiraTestCase(key, TestResult.FAILED);
                testCase.addFilePath(stackTraceFile);
                tests.add(testCase);
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult iTestResult) {
        super.onTestSkipped(iTestResult);
        String key = getTestJIRATestKey(iTestResult);
        if (key != null) {
            //todo add why...
            tests.add(new JiraTestCase(key, TestResult.BLOCKED));
        }
    }

    @Override
    public void onFinish(ITestContext iTestContext) {
        super.onFinish(iTestContext);
        for(ITestNGMethod method : iTestContext.getExcludedMethods()) {
            String key = getTestJIRATestKey(method);
            if (key != null) {
                tests.add(new JiraTestCase(key, TestResult.UNTESTED));
            }
        }

        XMLWriter.writeXmlFile(tests, "./target/tm-testng.xml");
    }

    private String getTestJIRATestKey(ITestResult result) {
        return getTestJIRATestKey(result.getMethod());
    }

    private String getTestJIRATestKey(ITestNGMethod testNGMethod) {
        Method method = testNGMethod.getConstructorOrMethod().getMethod();
        JIRATestKey annotation = method.getAnnotation(JIRATestKey.class);
        if (annotation != null) {
            return annotation.key();
        }
        return null;
    }

    private String [] getTestMethodDependencies(ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        Test annotation = method.getAnnotation(Test.class);
        if (annotation != null) {
            return annotation.dependsOnMethods();
        }
        return null;
    }

    private String [] getTestGroupsDependencies(ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        Test annotation = method.getAnnotation(Test.class);
        if (annotation != null) {
            return annotation.dependsOnGroups();
        }
        return null;
    }
}
