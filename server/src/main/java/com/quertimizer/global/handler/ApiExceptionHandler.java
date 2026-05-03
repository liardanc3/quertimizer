package com.quertimizer.global.handler;

import com.quertimizer.global.constant.GlobalFailReason;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.exception.DomainRuleViolationException;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleUnExpectedException(Exception exception) {
        return ResponseEntity
                .internalServerError()
                .body(ExceptionResponse.reasons(GlobalFailReason.UNEXPECTED_ERROR.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationException(MethodArgumentNotValidException exception) {
        List<String> reasons = extractReasons(exception.getBindingResult());

        return ResponseEntity
                .badRequest()
                .body(ExceptionResponse.reasons(reasons));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ExceptionResponse> handleBindException(BindException exception) {
        List<String> reasons = extractReasons(exception.getBindingResult());

        return ResponseEntity
                .badRequest()
                .body(ExceptionResponse.reasons(reasons));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ExceptionResponse> handleBusinessException(BusinessException exception) {
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(ExceptionResponse.reason(exception.getReason()));
    }

    @ExceptionHandler(DomainRuleViolationException.class)
    public ResponseEntity<ExceptionResponse> handleDomainRuleViolationException(DomainRuleViolationException exception) {
        return ResponseEntity
                .status(resolveStatus(exception))
                .body(ExceptionResponse.reason(exception.getMessage()));
    }

    @Getter
    public static class ExceptionResponse {

        private final List<String> reasons;

        private ExceptionResponse(List<String> reasons) {
            this.reasons = List.copyOf(reasons);
        }

        public static ExceptionResponse reasons(List<String> reasons) {
            // 여러 실패 사유를 그대로 응답 본문에 반영
            return new ExceptionResponse(reasons);
        }

        public static ExceptionResponse reasons(String... reasons) {
            // 가변 인자 실패 사유를 응답 목록으로 변환
            return new ExceptionResponse(Arrays.asList(reasons));
        }

        public static ExceptionResponse reason(String reason) {
            // 단일 실패 사유도 동일한 응답 구조로 래핑
            return new ExceptionResponse(List.of(reason));
        }
    }

    private List<String> extractReasons(BindingResult bindingResult) {
        // 검증 실패 필드별 메시지를 중복 없이 추출
        List<String> reasons = bindingResult
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .toList();

        // 실패 사유 추출 실패 시 기본값 설정
        if (reasons.isEmpty()) {
            reasons = List.of(GlobalFailReason.BAD_REQUEST.getMessage());
        }

        return reasons;
    }

    private HttpStatus resolveStatus(DomainRuleViolationException exception) {
        // 도메인 규칙 위반 유형을 HTTP 응답 상태로 변환
        return switch (exception.getType()) {
            case DUPLICATED_RESOURCE -> HttpStatus.CONFLICT;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case REQUEST_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
        };
    }
}
