package com.gym.pass.domain.membership;

/**
 * 정지 상한 (D-2 · D-24).
 * 값은 설정에서 읽어 application 계층이 넘긴다. 도메인은 설정을 직접 모른다.
 *
 * @param maxCount 회원권당 최대 정지 횟수 (시작 전 해제 건 제외)
 * @param daysPerMonth 개월당 누적 정지 일수 (누적 상한 = months × daysPerMonth)
 */
public record MembershipPauseLimits(int maxCount, int daysPerMonth) {

    public MembershipPauseLimits {
        if (maxCount < 1 || daysPerMonth < 1) {
            throw new IllegalArgumentException("정지 상한은 1 이상이어야 합니다.");
        }
    }

    /** 이 회원권의 누적 정지 일수 상한. */
    int maxTotalDays(int months) {
        return months * daysPerMonth;
    }
}
