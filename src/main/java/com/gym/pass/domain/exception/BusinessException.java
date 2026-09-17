package com.gym.pass.domain.exception;

import lombok.Getter;

/**
 * 비즈니스 예외 공통 베이스.
 * 도메인마다 domain/{domain}/exception/{Domain}Exception 하나만 두고 이 클래스를 상속한다.
 * <pre>
 * public class OrderException extends BusinessException {
 *     public OrderException(ErrorCode errorCode) { super(errorCode); }
 *     public OrderException(ErrorCode errorCode, String detail) { super(errorCode, detail); }
 * }
 * </pre>
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 기본 메시지 대신 상황별 설명을 응답에 담을 때 쓴다. */
    protected BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
