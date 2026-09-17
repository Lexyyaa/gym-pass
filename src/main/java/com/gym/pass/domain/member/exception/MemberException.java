package com.gym.pass.domain.member.exception;

import com.gym.pass.domain.exception.BusinessException;
import com.gym.pass.domain.exception.ErrorCode;

public class MemberException extends BusinessException {

    public MemberException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MemberException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
