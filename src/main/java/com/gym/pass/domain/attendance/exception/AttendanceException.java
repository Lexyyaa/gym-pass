package com.gym.pass.domain.attendance.exception;

import com.gym.pass.domain.exception.BusinessException;
import com.gym.pass.domain.exception.ErrorCode;

public class AttendanceException extends BusinessException {

    public AttendanceException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AttendanceException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
