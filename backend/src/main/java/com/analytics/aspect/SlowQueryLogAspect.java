package com.analytics.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class SlowQueryLogAspect {

    @Value("${slow-query.threshold-ms:500}")
    private long defaultThresholdMs;

    @Pointcut("@annotation(com.analytics.annotation.SlowQueryLog)")
    public void slowQueryPointcut() {
    }

    @Around("slowQueryPointcut()")
    public Object logSlowQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        long startNs = System.nanoTime();
        Object result = null;
        boolean success = true;
        String errorMsg = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            success = false;
            errorMsg = t.getClass().getSimpleName() + ": " + t.getMessage();
            throw t;
        } finally {
            long durationMs = (System.nanoTime() - startNs) / 1_000_000;
            long threshold = resolveThreshold(joinPoint);
            if (durationMs >= threshold) {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                String className = signature.getDeclaringType().getSimpleName();
                String methodName = signature.getName();
                String params = buildParamString(joinPoint.getArgs());

                if (success) {
                    log.warn(
                        "[SLOW QUERY] {}#{} took {}ms (threshold={}ms) | args=[{}] | resultType={}",
                        className, methodName, durationMs, threshold, params,
                        result != null ? result.getClass().getSimpleName() : "null"
                    );
                } else {
                    log.warn(
                        "[SLOW QUERY FAILED] {}#{} took {}ms (threshold={}ms) | args=[{}] | error={}",
                        className, methodName, durationMs, threshold, params, errorMsg
                    );
                }
            }
        }
    }

    private long resolveThreshold(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            SlowQueryLog annotation = method.getAnnotation(SlowQueryLog.class);
            if (annotation != null && annotation.thresholdMs() > 0) {
                return annotation.thresholdMs();
            }
        } catch (Exception ignored) {
        }
        return defaultThresholdMs;
    }

    private String buildParamString(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else {
                String s = arg.toString();
                if (s.length() > 200) {
                    s = s.substring(0, 197) + "...";
                }
                sb.append(arg.getClass().getSimpleName())
                  .append("=")
                  .append(s);
            }
        }
        return sb.toString();
    }

    public long getDefaultThresholdMs() {
        return defaultThresholdMs;
    }
}
