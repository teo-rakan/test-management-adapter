package com.epam.jira.core;


import java.util.ArrayList;
import java.util.List;

public class JiraTestCase {

    private String jiraTestKey;
    private TestResult status;
    private List<String> filePaths = new ArrayList<>();
    private List<String> comments = new ArrayList<>();

    public JiraTestCase(String jiraTestKey, TestResult status) {
        this.jiraTestKey = jiraTestKey;
        this.status = status;
    }

    public JiraTestCase(String jiraTestKey, String failedAssertMessage) {
        this.jiraTestKey = jiraTestKey;
        this.status = TestResult.FAILED;
        this.comments.add("Failed due to: " + failedAssertMessage);
    }

    public String getJiraTestKey() {
        return jiraTestKey;
    }

    public void setJiraTestKey(String jiraTestKey) {
        this.jiraTestKey = jiraTestKey;
    }

    public TestResult getStatus() {
        return status;
    }

    public void setStatus(TestResult status) {
        this.status = status;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public void addFilePath(String filePath) {
        this.filePaths.add(filePath);
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "{" + jiraTestKey + " status: " + status +  "}";
    }
}
