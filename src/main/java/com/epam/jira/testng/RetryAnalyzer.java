package com.epam.jira.testng;

import com.epam.jira.JIRATestKey;
import com.epam.jira.util.JiraInfoProvider;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.lang.reflect.Method;


public class RetryAnalyzer implements IRetryAnalyzer {

    private int counter = 0;
    private final int MAX_DEFAULT_ATTEMPTS = 1;

    boolean wasRerun() {
        return counter > 0;
    }

    int getRerunAttempts() {
        return counter;
    }

    @Override
    public boolean retry(ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        JIRATestKey annotation = method.getAnnotation(JIRATestKey.class);
        boolean hasJiraTestKey = annotation != null && !annotation.disabled();

        if (counter < (hasJiraTestKey ? annotation.retryCountIfFailed() : MAX_DEFAULT_ATTEMPTS)) {
            if (hasJiraTestKey) JiraInfoProvider.cleanFor(annotation.key());
            counter++;
            return true;
        }
        return false;
    }
}
