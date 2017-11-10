package com.epam.jira.util;

import com.epam.jira.JIRATestKey;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.Arrays;

class ReflectionUtils {

    private static CtMethod findMethod(String className, String methodName, int line) throws NotFoundException {
        CtClass clazz = ClassPool.getDefault().get(className);
        CtMethod[] methods = clazz.getDeclaredMethods();

        return Arrays.stream(methods)
                .filter(method -> method.getName().equals(methodName) && (method.getMethodInfo().getLineNumber(0) <= line))
                .sorted((m1, m2) -> Integer.compare(m2.getMethodInfo().getLineNumber(0), m1.getMethodInfo().getLineNumber(0)))
                .findFirst().orElse(null);
    }

    private static String findJiraKeyInCallStack(String className, String methodName, int line)  {
        try {
            CtMethod method = findMethod(className, methodName, line);
            Object jiraAnnotation = method.getAnnotation(JIRATestKey.class);
            if (jiraAnnotation != null) return ((JIRATestKey) jiraAnnotation).key();
        } catch (ClassNotFoundException | NotFoundException ignored) {
        }
        return null;
    }

    private static String findJiraKeyInCallStack(StackTraceElement[] trace, int depth)  {
        if (trace.length <= depth) return null;

        StackTraceElement traceElement = trace[depth];
        String className = traceElement.getClassName();
        String methodName = traceElement.getMethodName();
        int line = traceElement.getLineNumber();

        if (methodName.equals("invoke0")) return null;

        String key = findJiraKeyInCallStack(className, methodName, line);
        return key != null ? key : findJiraKeyInCallStack(trace, depth + 1);
    }

    public static String findJiraKeyInCallStack() {
        StackTraceElement[] trace = new Exception().getStackTrace();
        return findJiraKeyInCallStack(trace, 0);
    }
}
