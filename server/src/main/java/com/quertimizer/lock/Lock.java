package com.quertimizer.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Lock {

    LockKey prefix();

    String key() default "";

    // -1은 대기, 0은 즉시 시도, 양수는 timeout(ms)
    long timeout() default -1L;
}
