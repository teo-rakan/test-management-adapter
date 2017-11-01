package com.epam.jira.core;

public enum TestResult {

    FAILED("Failed"),
    PASSED("Passed"),
    BLOCKED("Blocked"),
    UNTESTED("Untested");

    private String text;

    TestResult(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
