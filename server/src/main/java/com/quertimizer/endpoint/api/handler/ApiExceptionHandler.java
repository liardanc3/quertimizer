package com.quertimizer.endpoint.api.handler;

import com.quertimizer.exception.BusinessException;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
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
        // 관리영역 외 오류 발생 시
        return ResponseEntity
                .internalServerError()
                .body(ExceptionResponse.reasons("잠시 후 다시 시도해주세요."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationException(MethodArgumentNotValidException exception) {

        // Request DTO Validation 실패 시 실패한 모든 필드 사유 포함하여 반환
        List<String> reasons = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .toList();

        // 실패 사유 추출 실패 시 기본값 설정
        if (reasons.isEmpty()) {
            reasons = List.of("잘못된 요청입니다.");
        }

        return ResponseEntity
                .badRequest()
                .body(ExceptionResponse.reasons(reasons));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ExceptionResponse> handleBusinessException(BusinessException exception) {

        // BusinessException 발생 시 선택한 상태코드와 이유 포함하여 반환
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
}
