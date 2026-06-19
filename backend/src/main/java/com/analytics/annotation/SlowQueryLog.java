package com.analytics.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlowQueryLog {

    long thresholdMs() default 500L;
}
