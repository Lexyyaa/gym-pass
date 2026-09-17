package com.gym.pass.domain.membership;

/** 회원권 종류. 종류별 정책(종료일 계산 · 차감 여부)은 F2에서 enum 메서드로 추가한다 (FR-2.4). */
public enum MembershipType {
    PERIOD,
    COUNT
}
