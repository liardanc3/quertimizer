package com.quertimizer.global.handler;

import com.quertimizer.global.constant.GlobalFailReason;
import com.quertimizer.global.exception.BusinessException;
import lombok.Getter;
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

    /**
     * 관리하지 않는 예외를 공용 500 응답으로 변환한다.
     *
     * @param exception 처리되지 않은 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleUnExpectedException(Exception exception) {
        return ResponseEntity
                .internalServerError()
                .body(ExceptionResponse.reasons(GlobalFailReason.UNEXPECTED_ERROR.getMessage()));
    }

    /**
     * Request DTO validation 예외를 공용 400 응답으로 변환한다.
     *
     * @param exception request body validation 예외
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationException(MethodArgumentNotValidException exception) {
        List<String> reasons = extractReasons(exception.getBindingResult());

        return ResponseEntity
                .badRequest()
                .body(ExceptionResponse.reasons(reasons));
    }

    /**
     * Query DTO binding 예외를 공용 400 응답으로 변환한다.
     *
     * @param exception query binding validation 예외
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ExceptionResponse> handleBindException(BindException exception) {
        List<String> reasons = extractReasons(exception.getBindingResult());

        return ResponseEntity
                .badRequest()
                .body(ExceptionResponse.reasons(reasons));
    }

    /**
     * BusinessException을 예외가 가진 상태 코드와 사유 응답으로 변환한다.
     *
     * @param exception 비즈니스 예외
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ExceptionResponse> handleBusinessException(BusinessException exception) {
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(ExceptionResponse.reason(exception.getReason()));
    }

    @Getter
    public static class ExceptionResponse {

        private final List<String> reasons;

        private ExceptionResponse(List<String> reasons) {
            this.reasons = List.copyOf(reasons);
        }

        public static ExceptionResponse reasons(List<String> reasons) {
            // 여러 실패 사유를 그대로 응답 본문에 담는다.
            return new ExceptionResponse(reasons);
        }

        public static ExceptionResponse reasons(String... reasons) {
            // 가변 인자 실패 사유를 응답 목록으로 변환한다.
            return new ExceptionResponse(Arrays.asList(reasons));
        }

        public static ExceptionResponse reason(String reason) {
            // 단일 실패 사유도 동일한 응답 구조로 감싼다.
            return new ExceptionResponse(List.of(reason));
        }
    }

    private List<String> extractReasons(BindingResult bindingResult) {
        // 검증 실패 필드별 메시지를 중복 없이 추출한다.
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
}
