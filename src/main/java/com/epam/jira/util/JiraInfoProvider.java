package com.epam.jira.util;

import com.epam.jira.JIRATestKey;
import com.epam.jira.entity.Parameter;
import org.apache.commons.lang3.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * JiraInfoProvider is a class responsible for gathering the issue artifacts information.
 * Basically the way it works is that we can find Jira Key in the method call stack using
 * ReflectionUtils and map it to some information (titled values or user files). This
 * mapped information would be requested by ExecutionListener during the result xml file
 * writing in order to add it to file for further Jenkins plugin processing.
 *
 * @author Alena_Zubrevich
 */
public class JiraInfoProvider {

    private static final Map<String, List<Parameter>> jiraKeyParametersMapping = new HashMap<>();
    private static final Map<String, List<String>> jiraKeyAttachmentsMapping = new HashMap<>();

    private static String findJiraTestKey() {
        Object jiraTestKeyObj = ReflectionUtils.findAnnotationInCallStack(JIRATestKey.class);
        if (jiraTestKeyObj != null && jiraTestKeyObj instanceof JIRATestKey) {
            return ((JIRATestKey) jiraTestKeyObj).key();
        }
        return null;
    }

    public static void saveFile(File file) {
        String key = findJiraTestKey();

        if (key == null) return;
        if (!file.exists() || !file.isFile()) return;

        // Copy to target if it's real file
        String currentDir = System.getProperty("user.dir");
        String targetFilePath = null;

        // Get relative file path if file placed in target directory or copy it there
        try {
            String filePath = file.getCanonicalPath();
            boolean placedOutOfTargetDir = !filePath.startsWith(currentDir + FileUtils.getTargetDir());
            targetFilePath = placedOutOfTargetDir
                    ? FileUtils.saveFile(file, file.getName())
                    : filePath.replaceFirst(currentDir, "");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Map issue key to file path
        if (jiraKeyAttachmentsMapping.containsKey(key)) {
            jiraKeyAttachmentsMapping.get(key).add(targetFilePath);
        } else {
            List<String> files = new ArrayList<>();
            files.add(targetFilePath);
            jiraKeyAttachmentsMapping.put(key, files);
        }
    }

    public static void saveValue(String title, String value) {
        String key = findJiraTestKey();

        if (key != null) {
            Parameter parameter = new Parameter(title, value != null ? value : null);
            if (jiraKeyParametersMapping.containsKey(key)) {
                jiraKeyParametersMapping.get(key).add(parameter);
            } else {
                List<Parameter> params = new ArrayList<>();
                params.add(parameter);
                jiraKeyParametersMapping.put(key, params);
            }
        }
    }

    public static void saveValue(String title, int value) {
        saveValue(title, String.valueOf(value));
    }

    public static void saveValue(String title, boolean value) {
        saveValue(title, String.valueOf(value));
    }

    public static void saveValue(String title, double value) {
        saveValue(title, String.valueOf(value));
    }

    public static void saveValue(String title, Object value) {
        saveValue(title, value != null ? value.toString() : "null");
    }

    public static List<String> getIssueAttachments(String key) {
        return jiraKeyParametersMapping.containsKey(key) ? jiraKeyAttachmentsMapping.get(key) : null;
    }

    public static List<Parameter> getIssueParameters(String key) {
        return jiraKeyParametersMapping.containsKey(key) ? jiraKeyParametersMapping.get(key) : null;
    }

}
