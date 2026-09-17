package com.gym.pass.domain.membership.exception;

import com.gym.pass.domain.exception.BusinessException;
import com.gym.pass.domain.exception.ErrorCode;

public class MembershipException extends BusinessException {

    public MembershipException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MembershipException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
