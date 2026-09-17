package com.gym.pass.support.exception;

import com.gym.pass.domain.exception.ErrorCode;

/**
 * 에러 응답 본문. { "errorCode": "...", "message": "..." }
 */
public record ErrorResponse(String errorCode, String message) {

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message);
    }
}
