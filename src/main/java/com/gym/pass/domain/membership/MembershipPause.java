package com.gym.pass.domain.membership;

import com.gym.pass.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원권 정지 (Membership 하위 엔티티, 03 §3.3).
 * membership_id는 Membership의 컬렉션 매핑이 채운다 (D-25 같은 애그리거트 안 물리 FK).
 * 정지 기간은 시작일 ~ 종료일 양끝 포함이고, 조기 해제하면 해제 당일까지가 유효 정지 구간이다 (D-8).
 */
@Getter
@Entity
@Table(
        name = "membership_pause",
        indexes = {
            @Index(name = "idx_membership_pause_start", columnList = "start_date"),
            @Index(name = "idx_membership_pause_membership", columnList = "membership_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipPause extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate startDate;

    /** 정지 종료일 = 시작일 + 일수 − 1 (당일 포함). */
    @Column(nullable = false)
    private LocalDate endDate;

    /** 조기 해제일. 해제하지 않았으면 null. */
    private LocalDate releasedDate;

    private MembershipPause(LocalDate startDate, int days) {
        this.startDate = startDate;
        this.endDate = startDate.plusDays(days - 1L);
    }

    /** 검증은 Membership.pause가 끝낸 뒤 호출한다. */
    static MembershipPause schedule(LocalDate startDate, int days) {
        return new MembershipPause(startDate, days);
    }

    /** 예정 정지 일수 (시작일 ~ 종료일, 양끝 포함). */
    public int plannedDays() {
        return daysBetweenInclusive(startDate, endDate);
    }

    /** 실제 사용 일수 (시작일 ~ 해제 당일, 양끝 포함). 해제 전이면 0, 시작 전 해제도 0 (D-8). */
    public int usedDays() {
        if (!isReleased() || releasedDate.isBefore(startDate)) {
            return 0;
        }
        return daysBetweenInclusive(startDate, releasedDate);
    }

    public boolean isReleased() {
        return releasedDate != null;
    }

    /** 누적 일수 상한 계산에 쓰는 일수. 해제된 건은 사용 일수, 아니면 예정 일수 (C-24 → D-2). */
    int countedDays() {
        return isReleased() ? usedDays() : plannedDays();
    }

    /** 정지 횟수에 세는지. 시작 전 해제(사용 0일)한 정지는 세지 않는다 (C-24 → D-2). */
    boolean countsTowardLimit() {
        return !isReleased() || usedDays() > 0;
    }

    /** 유효 정지 구간(해제 건은 시작일 ~ 해제일)이 [from, to]와 겹치는지 (03 §3.3). */
    boolean overlaps(LocalDate from, LocalDate to) {
        LocalDate effectiveEnd = effectiveEndDate();
        return !effectiveEnd.isBefore(startDate) && !from.isAfter(effectiveEnd) && !to.isBefore(startDate);
    }

    /** 오늘이 유효 정지 구간 안인지. 조기 해제 당일도 포함한다 (C-23 → D-8). */
    boolean covers(LocalDate day) {
        return overlaps(day, day);
    }

    /** 해제할 수 있는지. 이미 해제됐거나 정지 종료일이 지났으면 불가 (04 §4 PAUSE_NOT_RELEASABLE). */
    boolean isReleasableOn(LocalDate today) {
        return !isReleased() && !today.isAfter(endDate);
    }

    /** 오늘 해제한다. 미사용 일수(예정 − 사용)를 돌려준다. 가능 여부는 Membership이 먼저 확인한다. */
    int release(LocalDate today) {
        int unusedDays = unusedDaysIfReleasedOn(today);
        this.releasedDate = today;
        return unusedDays;
    }

    /** 오늘 해제한다면 되돌릴 미사용 일수. 상태를 바꾸지 않는다. 시작 전 해제면 예정 일수 전부다 (D-8). */
    int unusedDaysIfReleasedOn(LocalDate today) {
        int usedDays = today.isBefore(startDate) ? 0 : daysBetweenInclusive(startDate, today);
        return plannedDays() - usedDays;
    }

    /** 미해제 정지이면서 date보다 늦게 시작하는지 (C-40 → D-29 보충). */
    boolean isUnreleasedStartingAfter(LocalDate date) {
        return !isReleased() && startDate.isAfter(date);
    }

    private LocalDate effectiveEndDate() {
        return isReleased() ? releasedDate : endDate;
    }

    private static int daysBetweenInclusive(LocalDate from, LocalDate to) {
        return Math.toIntExact(ChronoUnit.DAYS.between(from, to) + 1);
    }
}
