package com.gym.pass.domain.membership;

/** 회원권 상태 (D-1 · FR-5.2). */
public enum MembershipStatus {
    ACTIVE,
    PAUSED,
    EXPIRED,
    CANCELED;

    /** 유효 회원권의 상태 범위인지 (ACTIVE · PAUSED, D-19). 만료 · 취소는 종결 상태다. */
    public boolean isUsable() {
        return this == ACTIVE || this == PAUSED;
    }
}
