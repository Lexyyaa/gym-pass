package com.gym.pass.domain.membership;

import com.gym.pass.domain.attendance.exception.AttendanceException;
import com.gym.pass.domain.branch.exception.BranchException;
import com.gym.pass.domain.common.BaseTimeEntity;
import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.domain.membership.exception.MembershipException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 회원권. 지점 귀속, 상태 컬럼 보유 (03 §3.3 · D-1 · D-10). */
@Getter
@Entity
@Table(
        name = "membership",
        indexes = {
            @Index(name = "idx_membership_branch_status_end", columnList = "branch_id, status, end_date"),
            @Index(name = "idx_membership_branch_end", columnList = "branch_id, end_date"),
            @Index(name = "idx_membership_member_status", columnList = "member_id, status"),
            @Index(name = "idx_membership_status_end", columnList = "status, end_date")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membership extends BaseTimeEntity {

    /** 저장 가능한 최소 날짜 (C-33 · MySQL DATE 보장 범위 하한). */
    private static final LocalDate MIN_DATE = LocalDate.of(1000, 1, 1);

    /** 저장 가능한 최대 날짜 (C-28 · MySQL DATE 상한). */
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long branchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    /** 개월 수. 기간제 = 등록 개월, 횟수제 = 6. 정지 상한 계산에 쓴다 (D-24). */
    @Column(nullable = false)
    private Integer months;

    private Integer totalCount;

    private Integer remainingCount;

    @Column(nullable = false)
    private Long price;

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false, updatable = false)
    @OrderBy("id ASC")
    private List<MembershipHistory> histories = new ArrayList<>();

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false, updatable = false)
    @OrderBy("id ASC")
    private List<MembershipPause> pauses = new ArrayList<>();

    private Membership(MembershipRegistration registration) {
        MembershipType membershipType = registration.type();
        this.memberId = registration.memberId();
        this.branchId = registration.branchId();
        this.type = membershipType;
        this.status = MembershipStatus.ACTIVE;
        this.startDate = registration.startDate();
        this.months = membershipType.validityMonths(registration.months());
        this.endDate = membershipType.calculateEndDate(registration.startDate(), registration.months());
        this.totalCount = membershipType.initialCount(registration.count());
        this.remainingCount = this.totalCount;
        this.price = registration.price();
    }

    /** 종료일을 계산해 ACTIVE로 생성하고 REGISTERED 이력을 남긴다 (FR-2.2 · FR-2.4 · FR-5.5). 상한은 설정값이다 (C-34). */
    public static Membership register(MembershipRegistration registration, MembershipLimits limits) {
        validate(registration, limits);
        Membership membership = new Membership(registration);
        membership.histories.add(MembershipHistory.registered(membership));
        return membership;
    }

    /** 다른 지점의 회원권이면 BRANCH_FORBIDDEN (NFR-1 · TC-3-08). */
    public void verifyBranch(Long requestBranchId) {
        if (!branchId.equals(requestBranchId)) {
            throw new BranchException(ErrorCode.BRANCH_FORBIDDEN);
        }
    }

    /** 출입 판정 대상 조건(ACTIVE · PAUSED, 종료일 ≥ today)을 만족하는지 (D-19 · 03 §7 조회 조건과 같다). */
    public boolean isEntryCandidateOn(LocalDate today) {
        return status.isUsable() && !today.isAfter(endDate);
    }

    /**
     * 출입 가능 판정 (FR-3.2 · FR-4.4 · H-10 · D-27). 저장 상태가 아니라 날짜 · 유효 정지 구간 · 잔여 · 오늘 차감 여부로 직접 검사한다.
     * 오늘이 유효 정지 구간 안이면(조기 해제 당일 포함, D-8) ATTENDANCE_MEMBERSHIP_PAUSED다.
     * 횟수제는 잔여 ≥ 1 또는 오늘(KST) 이미 차감된 출입이 있으면 허용한다.
     * deductedToday는 오늘 deducted=true 출입 기록이 있는지이며, membership 행 락을 잡은 뒤에 조회한 값이어야 한다.
     */
    public void validateEntry(LocalDate today, boolean deductedToday) {
        if (!isWithinPeriodOn(today)) {
            throw new AttendanceException(ErrorCode.ATTENDANCE_NO_VALID_MEMBERSHIP);
        }
        if (isPausedOn(today)) {
            throw new AttendanceException(ErrorCode.ATTENDANCE_MEMBERSHIP_PAUSED);
        }
        if (!hasEntryCountFor(deductedToday)) {
            throw new AttendanceException(ErrorCode.ATTENDANCE_NO_VALID_MEMBERSHIP);
        }
    }

    /**
     * 출입 1회분 차감 (FR-3.3 · FR-5.5). 차감이 일어났으면 true.
     * 차감 대상이 아닌 종류는 아무것도 바꾸지 않는다. 잔여 0에서는 차감할 수 없다.
     * 잔여가 0이 돼도 상태는 그대로 둔다. EXPIRED 전이는 00:00 상태 동기화 배치가 한다 (D-27).
     */
    public boolean deduct(LocalDate today) {
        validateEntry(today, false);
        if (!type.deductible()) {
            return false;
        }
        int before = remainingCount;
        remainingCount = before - 1;
        histories.add(MembershipHistory.deducted(this, before));
        return true;
    }

    /**
     * 정지 등록 (FR-4.1 ~ FR-4.3 · D-2 · D-8 · D-19 · D-28). 등록한 정지를 돌려준다.
     * 예정 일수만큼 종료일을 즉시 늘리고 PAUSED 이력을 남긴다. 시작일이 오늘이면 PAUSED로 전이한다 (03 §4).
     * 상한은 설정값이다. 검사 순서: 입력 → 소급 → 정지 가능 상태 → 횟수 → 누적 일수 → 겹침 → 날짜 범위.
     */
    public MembershipPause pause(
            LocalDate pauseStartDate, Integer days, LocalDate today, MembershipPauseLimits limits) {
        if (pauseStartDate == null || days == null || days < 1) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_INVALID_INPUT, "정지 시작일과 1일 이상의 정지 일수는 필수입니다.");
        }
        if (pauseStartDate.isBefore(today)) {
            throw new MembershipException(ErrorCode.PAUSE_START_DATE_PAST);
        }
        if (!isPausableOn(today)) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_NOT_PAUSABLE);
        }
        long pauseCount =
                pauses.stream().filter(MembershipPause::countsTowardLimit).count();
        if (pauseCount >= limits.maxCount()) {
            throw new MembershipException(ErrorCode.PAUSE_COUNT_LIMIT_EXCEEDED);
        }
        long totalDays = pauses.stream().mapToLong(MembershipPause::countedDays).sum() + days;
        if (totalDays > limits.maxTotalDays(months)) {
            throw new MembershipException(ErrorCode.PAUSE_DAYS_LIMIT_EXCEEDED);
        }
        LocalDate pauseEndDate = pauseStartDate.plusDays(days - 1L);
        if (pauses.stream().anyMatch(pause -> pause.overlaps(pauseStartDate, pauseEndDate))) {
            throw new MembershipException(ErrorCode.PAUSE_OVERLAPPED);
        }
        LocalDate extendedEndDate = endDate.plusDays(days);
        if (pauseEndDate.isAfter(MAX_DATE) || extendedEndDate.isAfter(MAX_DATE)) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_INVALID_INPUT, "정지 후 날짜가 " + MAX_DATE + "를 넘습니다.");
        }

        MembershipPause pause = MembershipPause.schedule(pauseStartDate, days);
        pauses.add(pause);
        LocalDate before = endDate;
        endDate = extendedEndDate;
        if (pauseStartDate.isEqual(today)) {
            status = MembershipStatus.PAUSED;
        }
        histories.add(MembershipHistory.endDateChanged(this, MembershipEventType.PAUSED, before));
        return pause;
    }

    /**
     * 정지 조기 해제 (FR-4.2 · D-8 · C-24 · C-25). 해제한 정지를 돌려준다.
     * 해제 당일까지 정지로 치고, 미사용 일수만큼 종료일을 되돌린 뒤 RESUMED 이력을 남긴다.
     * PAUSED 상태는 해제 대상을 뺀 다른 정지가 오늘을 포함하지 않으면 ACTIVE로 바꾼다 (03 §4).
     */
    public MembershipPause releasePause(Long pauseId, LocalDate today) {
        MembershipPause target = pauses.stream()
                .filter(pause -> pause.getId().equals(pauseId))
                .findFirst()
                .orElseThrow(() -> new MembershipException(ErrorCode.PAUSE_NOT_FOUND));
        if (!target.isReleasableOn(today)) {
            throw new MembershipException(ErrorCode.PAUSE_NOT_RELEASABLE);
        }

        int unusedDays = target.release(today);
        LocalDate before = endDate;
        endDate = endDate.minusDays(unusedDays);
        boolean otherPauseToday =
                pauses.stream().filter(pause -> pause != target).anyMatch(pause -> pause.covers(today));
        if (status == MembershipStatus.PAUSED && !otherPauseToday) {
            status = MembershipStatus.ACTIVE;
        }
        histories.add(MembershipHistory.endDateChanged(this, MembershipEventType.RESUMED, before));
        return target;
    }

    private boolean isPausedOn(LocalDate today) {
        return pauses.stream().anyMatch(pause -> pause.covers(today));
    }

    /** 만료 판정 = EXPIRED · CANCELED · 종료일 < 오늘 · 횟수제 잔여 0 (H-11 · D-21 · D-28). */
    private boolean isPausableOn(LocalDate today) {
        boolean exhausted = type.deductible() && (remainingCount == null || remainingCount < 1);
        return status.isUsable() && !today.isAfter(endDate) && !exhausted;
    }

    private boolean isWithinPeriodOn(LocalDate today) {
        return status.isUsable() && !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    private boolean hasEntryCountFor(boolean deductedToday) {
        return !type.deductible() || deductedToday || (remainingCount != null && remainingCount >= 1);
    }

    private static void validate(MembershipRegistration registration, MembershipLimits limits) {
        if (registration.memberId() == null || registration.branchId() == null) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_INVALID_INPUT, "회원과 지점은 필수입니다.");
        }
        if (registration.type() == null) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_INVALID_INPUT, "회원권 종류는 필수입니다.");
        }
        if (registration.startDate() == null) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_INVALID_INPUT, "시작일은 필수입니다.");
        }
        if (registration.price() == null || registration.price() < 0) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_INVALID_INPUT, "결제 금액은 0 이상이어야 합니다.");
        }
        registration.type().validate(registration.months(), registration.count(), limits);
        if (registration.startDate().isBefore(MIN_DATE)) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_INVALID_INPUT, "시작일은 " + MIN_DATE + " 이후여야 합니다.");
        }
        if (registration.startDate().isAfter(MAX_DATE)) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_INVALID_INPUT, "시작일은 " + MAX_DATE + " 이전이어야 합니다.");
        }
        LocalDate endDate = registration.type().calculateEndDate(registration.startDate(), registration.months());
        if (endDate.isAfter(MAX_DATE)) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_INVALID_INPUT, "종료일이 " + MAX_DATE + "를 넘습니다.");
        }
    }
}
