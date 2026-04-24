package com.quertimizer.global.lock;

import com.quertimizer.global.constant.GlobalFailReason;
import com.quertimizer.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LockAspect {

    private final LockManager lockManager;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(com.quertimizer.global.lock.Lock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 애노테이션 정보와 메서드 인자로 실제 lock key 계산
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Lock lockAnnotation = method.getAnnotation(Lock.class);
        String resolvedKey = resolveKey(method, joinPoint.getArgs(), lockAnnotation.key());
        String lockKey = buildLockKey(lockAnnotation.prefix(), resolvedKey);
        boolean locked = acquireLock(lockKey, lockAnnotation.timeout());

        // 동일 자원에 대한 동시 요청이면 바로 예외 반환
        if (!locked) {
            throw new BusinessException(GlobalFailReason.LOCK_ACQUIRE_FAILED.getMessage(), HttpStatus.LOCKED);
        }

        try {
            return joinPoint.proceed();
        } finally {
            lockManager.unlock(lockKey);
        }
    }

    private boolean acquireLock(String key, long timeout) {
        // timeout 정책에 따라 대기/즉시시도/시간제한시도 분기
        if (timeout < 0) {
            lockManager.lock(key);
            return true;
        }

        if (timeout == 0) {
            return lockManager.tryLock(key);
        }

        return lockManager.tryLock(key, timeout);
    }

    private String buildLockKey(LockKey prefix, String key) {
        // 락 키 구성
        if (key == null || key.isBlank()) {
            return prefix.toString();
        }

        return prefix + ":" + key;
    }

    private String resolveKey(Method method, Object[] args, String expression) {
        // 키 결정
        if (expression == null || expression.isBlank()) {
            return "";
        }

        // #이 없으면 고정 문자열 key로 사용
        if (!expression.contains("#")) {
            return expression;
        }

        // SpEL 표현식이면 메서드 인자를 바인딩해 key 계산
        MethodBasedEvaluationContext context =
                new MethodBasedEvaluationContext(null, method, args, parameterNameDiscoverer);

        Object value = expressionParser.parseExpression(expression).getValue(context);
        if (value == null) {
            throw new IllegalArgumentException(GlobalFailReason.LOCK_KEY_RESOLVE_FAILED.format(expression));
        }

        return value.toString();
    }
}
