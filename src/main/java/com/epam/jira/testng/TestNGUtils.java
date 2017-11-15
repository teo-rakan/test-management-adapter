package com.epam.jira.testng;

import com.epam.jira.JIRATestKey;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

class TestNGUtils {

    static String getFullMethodName (ITestResult result) {
        String methodName = result.getMethod().getConstructorOrMethod().getMethod().getName();
        return result.getMethod().getTestClass().getName() + "." + methodName;
    }

    static String [] getMethodGroups (ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        Test annotation = method.getAnnotation(Test.class);
        if (annotation != null) {
            return annotation.groups();
        }
        return null;
    }

    static String [] getTestMethodDependencies(Method method) {
        Test annotation = method.getAnnotation(Test.class);
        if (annotation != null) {
            return annotation.dependsOnMethods();
        }
        return null;
    }

    static String [] getTestGroupsDependencies(Method method) {
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

    static String getTimeAsString(ITestResult result) {
        long timeDiff = result.getEndMillis() - result.getStartMillis();
        String formattedResult;
        if (timeDiff < 10)
            formattedResult = timeDiff + " ms";
        else if (timeDiff < 60000)
            formattedResult = (timeDiff / 1000.0) + " s";
        else
            formattedResult = (timeDiff / 60000.0) + " min";
        return formattedResult;
    }
}
