package com.gym.pass.domain.notification;

/** 발송 결과 (D-3). 포기는 FAILED + attemptCount == 3으로 표현한다. */
public enum NotificationStatus {
    SENT,
    FAILED
}
