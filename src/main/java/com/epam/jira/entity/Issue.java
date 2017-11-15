package com.epam.jira.entity;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "test")
@XmlAccessorType(XmlAccessType.FIELD)
public class Issue {

    @XmlElement(name = "key", required = true)
    private String issueKey;

    @XmlElement(name = "status", required = true)
    private String status;

    @XmlElement(name = "summary")
    private String summary;

    @XmlElement(name = "time")
    private String time;

    @XmlElementWrapper(name = "attachments")
    @XmlElement(name = "attachment")
    private List<String> attachments;

    @XmlElementWrapper(name = "parameters")
    @XmlElement(name = "parameter")
    private List<Parameter> parameters;

    public Issue(String issueKey, TestResult status) {
        this.issueKey = issueKey;
        this.status = status.toString();
    }

    public Issue(String issueKey, TestResult status, String time) {
        this.issueKey = issueKey;
        this.status = status.toString();
        this.time = time;
    }

    public Issue() {
    }

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

}
