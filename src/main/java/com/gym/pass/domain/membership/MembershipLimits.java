package com.gym.pass.domain.membership;

/**
 * 회원권 등록 입력 상한 (C-28 · C-34 · D-23).
 * 값은 설정에서 읽어 application 계층이 넘긴다. 도메인은 설정을 직접 모른다.
 */
public record MembershipLimits(int maxMonths, int maxCount) {

    public MembershipLimits {
        if (maxMonths < 1 || maxCount < 1) {
            throw new IllegalArgumentException("회원권 입력 상한은 1 이상이어야 합니다.");
        }
    }
}
