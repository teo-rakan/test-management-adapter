package com.epam.jira.util;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.Arrays;


/**
 * ReflectionUtils is a util class responsible for finding Jira issue key in the method call
 * stack. The first thing we have to do is to get Stack Trace information using new exception
 * creating. After that, we sequentially check methods for JiraTestKey annotation. The only
 * catch is that we have information only about class name, method name and call line. But
 * that's not enough for getting method as Reflection API object because of the lack of method
 * parameters information. So we have to get all class methods with defined name and theirs
 * start line numbers using Javassist possibilities. After that we can determine exactly
 * what method was called.
 *
 * @author Alena_Zubrevich
 */
class ReflectionUtils {

    private static CtMethod findMethod(String className, String methodName, int line) throws NotFoundException {
        CtClass clazz = ClassPool.getDefault().get(className);
        CtMethod[] methods = clazz.getDeclaredMethods();

        return Arrays.stream(methods)
                .filter(method -> method.getName().equals(methodName) && (method.getMethodInfo().getLineNumber(0) <= line))
                .sorted((m1, m2) -> Integer.compare(m2.getMethodInfo().getLineNumber(0), m1.getMethodInfo().getLineNumber(0)))
                .findFirst().orElse(null);
    }

    private static Object findAnnotationInCallStack(Class annotationClass, String className, String methodName, int line)  {
        try {
            CtMethod method = findMethod(className, methodName, line);
            Object annotation = method.getAnnotation(annotationClass);
            if (annotation != null) return annotation;
        } catch (ClassNotFoundException | NotFoundException ignored) {
        }
        return null;
    }

    private static Object findAnnotationInCallStack(Class annotationClass, StackTraceElement[] trace, int depth)  {
        if (trace.length <= depth) return null;

        StackTraceElement traceElement = trace[depth];
        String className = traceElement.getClassName();
        String methodName = traceElement.getMethodName();
        int line = traceElement.getLineNumber();

        if (methodName.equals("invoke0")) return null;

        Object annotation = findAnnotationInCallStack(annotationClass, className, methodName, line);
        return annotation != null ? annotation : findAnnotationInCallStack(annotationClass, trace, depth + 1);
    }

    static Object findAnnotationInCallStack(Class annotationClass) {
        StackTraceElement[] trace = new Exception().getStackTrace();
        return findAnnotationInCallStack(annotationClass, trace, 2);
    }
}
