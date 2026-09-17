package com.gym.pass.domain.membership;

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

    private Membership(MembershipRegistration registration) {
        MembershipType membershipType = registration.type();
        this.memberId = registration.memberId();
        this.branchId = registration.branchId();
        this.type = membershipType;
        this.status = MembershipStatus.ACTIVE;
        this.startDate = registration.startDate();
        this.months = membershipType.validityMonths(registration.months());
        this.endDate = registration.startDate().plusMonths(this.months);
        this.totalCount = membershipType.initialCount(registration.count());
        this.remainingCount = this.totalCount;
        this.price = registration.price();
    }

    /** 종료일을 계산해 ACTIVE로 생성하고 REGISTERED 이력을 남긴다 (FR-2.2 · FR-2.4 · FR-5.5). */
    public static Membership register(MembershipRegistration registration) {
        validate(registration);
        Membership membership = new Membership(registration);
        membership.histories.add(MembershipHistory.registered(membership));
        return membership;
    }

    private static void validate(MembershipRegistration registration) {
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
        registration.type().validate(registration.months(), registration.count());
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
