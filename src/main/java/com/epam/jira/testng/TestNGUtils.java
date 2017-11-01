package com.epam.jira.testng;

import com.epam.jira.JIRATestKey;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

class TestNGUtils {

    static String getMethodName (ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        return method.getName();
    }

    static String [] getMethodGroups (ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        Test annotation = method.getAnnotation(Test.class);
        if (annotation != null) {
            return annotation.groups();
        }
        return null;
    }

    static String [] getTestMethodDependencies(ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        Test annotation = method.getAnnotation(Test.class);
        if (annotation != null) {
            return annotation.dependsOnMethods();
        }
        return null;
    }

    static String [] getTestGroupsDependencies(ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        Test annotation = method.getAnnotation(Test.class);
        if (annotation != null) {
            return annotation.dependsOnGroups();
        }
        return null;
    }

    static String getTestJIRATestKey(ITestResult result) {
        return getTestJIRATestKey(result.getMethod());
    }

    static String getTestJIRATestKey(ITestNGMethod testNGMethod) {
        Method method = testNGMethod.getConstructorOrMethod().getMethod();
        JIRATestKey annotation = method.getAnnotation(JIRATestKey.class);
        if (annotation != null) {
            return annotation.key();
        }
        return null;
    }
}
