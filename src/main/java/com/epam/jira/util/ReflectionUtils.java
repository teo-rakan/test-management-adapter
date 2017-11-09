package com.epam.jira.util;

import com.epam.jira.JIRATestKey;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.Arrays;

public class ReflectionUtils {

    private static CtMethod findMethod(String className, String methodName, int line) throws NotFoundException {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get(className);
        CtMethod[] methods = cc.getDeclaredMethods();

        return Arrays.stream(methods)
                .filter(method -> method.getName().equals(methodName) && (method.getMethodInfo().getLineNumber(0) <= line))
                .sorted((m1, m2) -> - Integer.compare(m1.getMethodInfo().getLineNumber(0),m2.getMethodInfo().getLineNumber(0))).findFirst().orElse(null);
    }

    private static String findJiraKeyInCallStack(String className, String methodName, int line) throws NotFoundException {
        CtMethod method = findMethod(className, methodName, line);
        try {
            JIRATestKey jiraAnnotation = (JIRATestKey) method.getAnnotation(JIRATestKey.class);
            if (jiraAnnotation != null)
                return jiraAnnotation.key();
        } catch (ClassNotFoundException e) {
        }
        return null;
    }

    private static String findJiraKeyInCallStack(StackTraceElement[] trace, int depth) throws NotFoundException {
        if (trace.length <= depth) return null;

        StackTraceElement traceElement = trace[depth];
        String className = traceElement.getClassName();
        String methodName = traceElement.getMethodName();
        int line = traceElement.getLineNumber();

        if (methodName.equals("invoke0")) return null;

        String key = findJiraKeyInCallStack(className, methodName, line);
        if (key != null) return key;

        return findJiraKeyInCallStack(trace, depth + 1);
    }

    public static String findJiraKeyInCallStack()  {
        StackTraceElement[] trace = new Exception().getStackTrace();
        String key = null;
        try {
            key = findJiraKeyInCallStack(trace, 0);
        } catch (NotFoundException ignored) {
        }
        return key;
    }
}
