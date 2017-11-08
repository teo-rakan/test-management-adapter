package com.epam.jira.util;

import com.epam.jira.JIRATestKey;
import com.epam.jira.entity.Parameter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JiraInfoProvider {

    private static Map<String, List<Parameter>> jiraKeyParametersMapping = new HashMap<>();
    private static Map<String, List<String>> jiraKeyAttachmentsMapping = new HashMap<>();
    private final static int MIN_CALL_DEPTH = 4;

    private static String getCallerMethodJiraKey(int depth) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        return getCallerMethodJiraKey(trace, depth);
    }

    private static String getCallerMethodJiraKey(StackTraceElement[] trace, int depth) {
        if (trace.length <= depth) return null;

        String className = trace[depth].getClassName();
        String methodName = trace[depth].getMethodName();

        if (methodName.equals("invoke0")) return null;

        try {
            Class clazz = Class.forName(className);
            Method stackMethod = null;

            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName)) {
                    if (stackMethod != null)
                        throw new RuntimeException("Cannot resolve method by name (multiple methods were found): " + methodName);
                    stackMethod = method;
                }
            }
            JIRATestKey jiraKey = stackMethod.getAnnotation(JIRATestKey.class);
            if (jiraKey != null) return jiraKey.key();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot find class " + className + " from StackTrace element: " + trace[depth]);
        }
        return getCallerMethodJiraKey(trace, depth + 1);
    }

    private static String getCallerMethodJiraKey() {
        return getCallerMethodJiraKey(MIN_CALL_DEPTH);
    }

    public static String saveFile(File file) {
        String key = getCallerMethodJiraKey();

        if (key == null) return null;
        if (!file.exists() || !file.isFile()) return null;
        //todo save virtual ?

        String currentDir = System.getProperty("user.dir");
        String targetFilePath = null;

        // Get relative path or copy to target
        try {
            String filePath = file.getCanonicalPath();
            boolean placedOutOfTargetDir = !filePath.startsWith(currentDir + FileUtils.getTargetDir());
            targetFilePath = placedOutOfTargetDir
                    ? FileUtils.saveFile(file, file.getName())
                    : filePath.replaceFirst(currentDir, "");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Map file path
        if (jiraKeyAttachmentsMapping.containsKey(key)) {
            jiraKeyAttachmentsMapping.get(key).add(targetFilePath);
        } else {
            List<String> files = new ArrayList<>();
            files.add(targetFilePath);
            jiraKeyAttachmentsMapping.put(key, files);
        }

        return targetFilePath;
    }

    public static void saveValue(String title, String value) {
        String key = getCallerMethodJiraKey();

        if (key != null) {
            Parameter parameter = new Parameter(title, value);
            if (jiraKeyParametersMapping.containsKey(key)) {
                jiraKeyParametersMapping.get(key).add(parameter);
            } else {
                List<Parameter> params = new ArrayList<>();
                params.add(parameter);
                jiraKeyParametersMapping.put(key, params);
            }
        }
    }

    public static List<String> getIssueAttachments(String key) {
        return jiraKeyParametersMapping.containsKey(key) ? jiraKeyAttachmentsMapping.get(key) : null;
    }

    public static List<Parameter> getIssueParameters(String key) {
        return jiraKeyParametersMapping.containsKey(key) ? jiraKeyParametersMapping.get(key) : null;
    }

}
