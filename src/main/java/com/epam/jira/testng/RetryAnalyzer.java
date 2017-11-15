package com.epam.jira.testng;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.lang.reflect.Method;


public class RetryAnalyzer implements IRetryAnalyzer {

    private int counter = 0;
    private final int MAX_DEFAULT_ATTEMPTS = 1;

    public boolean wasRerun() {
        return counter > 0;
    }

    public int getRerunAttempts() {
        return counter;
    }

    @Override
    public boolean retry(ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        RetryCountIfFailed annotation = method.getAnnotation(RetryCountIfFailed.class);

        if (counter < (annotation != null ? annotation.value() : MAX_DEFAULT_ATTEMPTS)) {
            counter++;
            return true;
        }
        return false;
    }
}
