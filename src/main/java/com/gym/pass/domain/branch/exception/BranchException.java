package com.gym.pass.domain.branch.exception;

import com.gym.pass.domain.exception.BusinessException;
import com.gym.pass.domain.exception.ErrorCode;

public class BranchException extends BusinessException {

    public BranchException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BranchException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
