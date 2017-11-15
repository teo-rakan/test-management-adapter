package com.epam.jira.testng;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryCountIfFailed {
    int value() default 0;
}
