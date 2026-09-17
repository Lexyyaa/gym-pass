package com.gym.pass.domain.exception;

/** 특정 도메인에 속하지 않는 공통 예외 (지점 헤더 형식 오류 등). */
public class CommonException extends BusinessException {

    public CommonException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CommonException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
