package com.epam.jira;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface JIRATestKey {
    String key();
    boolean disabled() default false;
    int retryCountIfFailed() default 1;
    boolean screenshotDisabled() default false;
}
