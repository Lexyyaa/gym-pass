package com.gym.pass.support.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 회원권 정책 값 (C-34 · D-23 · D-2).
 *
 * @param maxMonths 기간제 개월 수 상한
 * @param maxCount 횟수제 이용 횟수 상한
 * @param pause 정지 상한
 */
@Validated
@ConfigurationProperties(prefix = "gym.membership")
public record MembershipProperties(
        @Positive int maxMonths,
        @Positive int maxCount,
        @Valid @NotNull Pause pause) {

    /**
     * 정지 상한 (D-2 · D-24).
     *
     * @param maxCount 회원권당 최대 정지 횟수
     * @param daysPerMonth 개월당 누적 정지 일수
     */
    public record Pause(@Positive int maxCount, @Positive int daysPerMonth) {}
}
