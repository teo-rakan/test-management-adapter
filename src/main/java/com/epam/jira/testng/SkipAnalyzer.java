package com.epam.jira.testng;

import com.epam.jira.entity.Issue;
import com.epam.jira.util.FileUtils;
import org.testng.ITestResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;


class SkipAnalyzer {
    private final Map<String, Issue> failedMethods = new HashMap<>();
    private final Map<String, List<Issue>> failedGroups = new HashMap<>();
    private final List<String> failedConfigs = new ArrayList<>();

    void addFailedConfig(ITestResult result) {
        Annotation[] annotations = result.getMethod().getConstructorOrMethod().getMethod().getDeclaredAnnotations();
        String annotationInfo = annotationsToString(annotations);
        failedConfigs.add(TestNGUtils.getFullMethodName(result) + annotationInfo + ".\n" + FileUtils.save(result.getThrowable()));
    }

    void addFailedResult(ITestResult result, Issue issue) {
        String[] groups = TestNGUtils.getMethodGroups(result);
        String methodName = TestNGUtils.getFullMethodName(result);

        failedMethods.put(methodName, issue);
        for (String group : groups) {
            if (failedGroups.containsKey(group)) {
                if (issue != null) {
                    List<Issue> issues = failedGroups.get(group);
                    if (issues != null) issues.add(issue);
                }
            } else {
                List<Issue> failedIssues = null;
                if (issue != null) {
                    failedIssues = new ArrayList<>();
                    failedIssues.add(issue);
                }
                failedGroups.put(group, failedIssues);
            }
        }
    }

    String getLastFailedConfig() {
        return failedConfigs.isEmpty() ? null : failedConfigs.get(failedConfigs.size() - 1);
    }


    String getReasonIfHaveDependencies(ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        StringBuilder blockReasons = new StringBuilder();
        String className = method.getDeclaringClass().getName();
        String methodName = className + "." + method.getName();
        Integer dependencyCounter = 0;

        blockReasons.append("Test method ").append(methodName).append(" (%s) depends on");
        dependencyCounter = addMethodDependencies(dependencyCounter, blockReasons, method);
        dependencyCounter = addGroupDependencies(dependencyCounter, blockReasons, method);

        return (dependencyCounter > 0) ? blockReasons.toString() : null;
    }

    private int addMethodDependencies(int dependencyCounter, StringBuilder builder, Method testMethod) {
        String[] methods = TestNGUtils.getTestMethodDependencies(testMethod);
        String testClassName = testMethod.getDeclaringClass().getName();
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

    private int addGroupDependencies(int dependencyCounter, StringBuilder builder, Method testMethod) {
        String[] groups = TestNGUtils.getTestGroupsDependencies(testMethod);
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

    private String annotationsToString(Annotation[] annotations) {
        if (annotations == null || annotations.length == 0) return "";
        return Arrays.stream(annotations).map(a -> a.annotationType().getSimpleName()).collect(
                Collectors.joining(", @", " annotated with @", ""));
    }
}
