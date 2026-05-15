package com.quertimizer.global.log;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@Order
public class LogAspect {

    @Around("@annotation(com.quertimizer.global.log.Log)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 애노테이션 정보로 실제 로그 메시지 조회
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Log logAnnotation = method.getAnnotation(Log.class);
        String message = logAnnotation.value();

        try {
            // 실행 시작 로그 출력 후 대상 메서드 실행
            log.info("{} 시작", message);
            Object result = joinPoint.proceed();

            // 실행 완료 로그 출력 후 결과 반환
            log.info("{} 완료", message);
            return result;
        } catch (Throwable throwable) {
            // 실행 오류 로그 출력 후 기존 예외 전파
            log.error("{} 오류 발생", message, throwable);
            throw throwable;
        }
    }
}
