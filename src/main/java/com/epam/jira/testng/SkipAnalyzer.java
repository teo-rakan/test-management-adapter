package com.epam.jira.testng;

import com.epam.jira.entity.Issue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Alena_Zubrevich on 11/15/2017.
 */
public class SkipAnalyzer {


    public String getReasonIfHaveDependencies (Method testMethod, Map<String, Issue> failedMethods, Map<String, List<Issue>> failedGroups) {
        StringBuilder blockReasons = new StringBuilder();
        String className = testMethod.getDeclaringClass().getName();
        String methodName = className + "." + testMethod.getName();
        Integer dependencyCounter = 0;

        blockReasons.append("Test method ").append(methodName).append(" (%s) depends on");
        dependencyCounter = addMethodDependencies(failedMethods, dependencyCounter, blockReasons, testMethod);
        dependencyCounter = addGroupDependencies(failedGroups, dependencyCounter, blockReasons, testMethod);

        return (dependencyCounter > 0) ? blockReasons.toString() : null;
    }

    private int addMethodDependencies(Map<String, Issue> failedMethods, int dependencyCounter, StringBuilder builder, Method testMethod) {
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

    private int addGroupDependencies(Map<String, List<Issue>> failedGroups, int dependencyCounter, StringBuilder builder, Method testMethod) {
        String[] groups =  TestNGUtils.getTestGroupsDependencies(testMethod);
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
}
