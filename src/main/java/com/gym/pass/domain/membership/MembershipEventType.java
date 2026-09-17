package com.gym.pass.domain.membership;

/** 회원권 변경 이력 이벤트 (FR-5.5). 연장은 PAUSED · RESUMED의 종료일 before/after로 기록한다. */
public enum MembershipEventType {
    REGISTERED,
    PAUSED,
    RESUMED,
    DEDUCTED,
    CANCELED
}
