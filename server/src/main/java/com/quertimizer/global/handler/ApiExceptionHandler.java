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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleUnExpectedException(Exception exception) {

        // 관리영역 외의 에러 발생 시 500 반환
        return ResponseEntity
                .internalServerError()
                .body(ExceptionResponse.reasons(GlobalFailReason.UNEXPECTED_ERROR.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationException(MethodArgumentNotValidException exception) {

        // Request DTO Validation 실패 시 실패한 모든 필드 사유 포함하여 반환
        List<String> reasons = extractReasons(exception.getBindingResult());

        return ResponseEntity
                .badRequest()
                .body(ExceptionResponse.reasons(reasons));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ExceptionResponse> handleBindException(BindException exception) {

        // Query DTO Validation 실패 시 실패한 모든 필드 사유 포함하여 반환
        List<String> reasons = extractReasons(exception.getBindingResult());

        return ResponseEntity
                .badRequest()
                .body(ExceptionResponse.reasons(reasons));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ExceptionResponse> handleBusinessException(BusinessException exception) {

        // BusinessException 발생 시 상태코드와 이유 포함하여 반환
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
            return new ExceptionResponse(reasons);
        }

        public static ExceptionResponse reasons(String... reasons) {
            return new ExceptionResponse(Arrays.asList(reasons));
        }

        public static ExceptionResponse reason(String reason) {
            return new ExceptionResponse(List.of(reason));
        }
    }

    private List<String> extractReasons(BindingResult bindingResult) {
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
