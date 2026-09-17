package com.gym.pass.support.exception;

import com.gym.pass.domain.exception.BusinessException;
import com.gym.pass.domain.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 예외를 ErrorResponse로 바꾸는 단일 지점.
 * - BusinessException → ErrorCode의 HTTP 상태
 * - 입력 형식 오류(프레임워크 예외) → 400 COMMON_INVALID_INPUT
 * - 그 외 → 500 COMMON_INTERNAL_SERVER_ERROR (입력 오류가 여기로 오면 버그다)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("비즈니스 예외(서버 측): {}", errorCode, e);
        } else {
            log.info("비즈니스 예외: {} - {}", errorCode, e.getMessage());
        }
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleNotValid(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::describe)
                .findFirst()
                .orElse(ErrorCode.COMMON_INVALID_INPUT.getMessage());
        log.info("입력값 검증 실패: {}", detail);
        return invalidInput(detail);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException e) {
        String detail = e.getParameterValidationResults().stream()
                .findFirst()
                .map(result -> result.getMethodParameter().getParameterName() + ": "
                        + result.getResolvableErrors().get(0).getDefaultMessage())
                .orElse(ErrorCode.COMMON_INVALID_INPUT.getMessage());
        log.info("입력값 검증 실패: {}", detail);
        return invalidInput(detail);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        ServletRequestBindingException.class,
        MissingServletRequestPartException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        HttpMediaTypeNotSupportedException.class,
        MultipartException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidInput(Exception e) {
        log.info("입력 형식 오류: {}", e.getMessage());
        return invalidInput(ErrorCode.COMMON_INVALID_INPUT.getMessage());
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNoResource(Exception e) {
        return respond(ErrorCode.COMMON_RESOURCE_NOT_FOUND);
    }

    /** 클라이언트가 JSON을 받지 않겠다고 한 경우. 본문을 만들 수 없으므로 상태만 돌려준다. */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleNotAcceptable(HttpMediaTypeNotAcceptableException e) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return respond(ErrorCode.COMMON_METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 서버 오류", e);
        return respond(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
    }

    private static ResponseEntity<ErrorResponse> invalidInput(String detail) {
        return ResponseEntity.status(ErrorCode.COMMON_INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_INVALID_INPUT, detail));
    }

    private static ResponseEntity<ErrorResponse> respond(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.from(errorCode));
    }

    private static String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
