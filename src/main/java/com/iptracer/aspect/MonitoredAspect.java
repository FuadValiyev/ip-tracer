package com.iptracer.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class MonitoredAspect {

    @Before("execution(public * *(..)) && @within(Monitored)")
    public void beforeAdvice(JoinPoint joinPoint) {
        StringBuilder logString = new StringBuilder();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = methodSignature.getDeclaringTypeName();
        String methodName = methodSignature.getName();
        Object[] arguments = joinPoint.getArgs();

        logString.append("STARTED ")
                .append("Class: ").append(className)
                .append(", Method: ").append(methodName)
                .append(", Params: [").append(Arrays.toString(arguments)).append("]");
        log.info(logString.toString());
    }

    @AfterReturning(pointcut = "execution(public * *(..)) && @within(Monitored)", returning = "result")
    public void afterReturningAdvice(JoinPoint joinPoint, Object result) {
        StringBuilder logString = new StringBuilder();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = methodSignature.getDeclaringTypeName();
        String methodName = methodSignature.getName();

        logString.append("END ")
                .append("Class: ").append(className)
                .append(", Method: ").append(methodName)
                .append(", Returned: ").append(result);

        log.info(logString.toString());
    }

    @AfterThrowing(pointcut = "execution(public * *(..)) && @within(Monitored)", throwing = "e")
    public void myAfterThrowing(JoinPoint joinPoint, Exception e) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        StringBuilder logString = new StringBuilder();
        String className = methodSignature.getDeclaringTypeName();
        String methodName = methodSignature.getName();

        logString.append("ERROR ")
                .append("Class: ").append(className)
                .append(", Method: ").append(methodName)
                .append(", Exception: ").append(e.toString());
        log.error(logString.toString());
    }
}