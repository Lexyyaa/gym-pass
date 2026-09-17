package com.gym.pass.domain.membership;

import com.gym.pass.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원권 변경 이력 (append-only, FR-5.5 · 03 §3.3).
 * Membership 행위 안에서 생성되고, membership_id는 Membership의 컬렉션 매핑이 채운다.
 */
@Getter
@Entity
@Table(
        name = "membership_history",
        indexes = {
            @Index(
                    name = "idx_membership_history_member_branch_created",
                    columnList = "member_id, branch_id, created_at")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "membership_id", nullable = false, insertable = false, updatable = false)
    private Long membershipId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long branchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipEventType eventType;

    private LocalDate endDateBefore;

    private LocalDate endDateAfter;

    private Integer remainingCountBefore;

    private Integer remainingCountAfter;

    private MembershipHistory(
            Long memberId,
            Long branchId,
            MembershipEventType eventType,
            LocalDate endDateBefore,
            LocalDate endDateAfter,
            Integer remainingCountBefore,
            Integer remainingCountAfter) {
        this.memberId = memberId;
        this.branchId = branchId;
        this.eventType = eventType;
        this.endDateBefore = endDateBefore;
        this.endDateAfter = endDateAfter;
        this.remainingCountBefore = remainingCountBefore;
        this.remainingCountAfter = remainingCountAfter;
    }

    /** 등록 이벤트. 변경 전 값은 없고, 변경 후 종료일 · 잔여 횟수만 남긴다. */
    static MembershipHistory registered(Membership membership) {
        return new MembershipHistory(
                membership.getMemberId(),
                membership.getBranchId(),
                MembershipEventType.REGISTERED,
                null,
                membership.getEndDate(),
                null,
                membership.getRemainingCount());
    }
}
