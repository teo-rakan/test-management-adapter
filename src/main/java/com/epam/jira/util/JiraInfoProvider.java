package com.epam.jira.util;

import com.epam.jira.entity.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JiraInfoProvider {

    private static final Map<String, List<Parameter>> jiraKeyParametersMapping = new HashMap<>();
    private static final Map<String, List<String>> jiraKeyAttachmentsMapping = new HashMap<>();

    public static String saveFile(File file) {
        String key = ReflectionUtils.findJiraKeyInCallStack();

        if (key == null) return null;
        if (!file.exists() || !file.isFile()) return null;

        // Copy to target if it's real file
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
        String key = ReflectionUtils.findJiraKeyInCallStack();

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
