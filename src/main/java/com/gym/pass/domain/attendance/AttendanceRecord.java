package com.gym.pass.domain.attendance;

import com.gym.pass.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 출입 기록. 지점 귀속, 회원 · 회원권 · 지점은 논리 참조다 (03 §3.4 · D-10 · D-25). */
@Getter
@Entity
@Table(
        name = "attendance_record",
        indexes = {
            @Index(name = "idx_attendance_membership_entry_date", columnList = "membership_id, entry_date"),
            @Index(name = "idx_attendance_member_branch_entry_at", columnList = "member_id, branch_id, entry_at")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long membershipId;

    @Column(nullable = false)
    private Long branchId;

    /** 서버 시각(KST). 클라이언트 입력을 받지 않는다. */
    @Column(nullable = false)
    private LocalDateTime entryAt;

    /** entryAt의 KST 달력일. 하루 1회 차감 판정 기준 (D-5). */
    @Column(nullable = false)
    private LocalDate entryDate;

    /** 이 출입에서 차감이 일어났는지. */
    @Column(nullable = false)
    private boolean deducted;

    private AttendanceRecord(Long memberId, Long membershipId, Long branchId, LocalDateTime entryAt, boolean deducted) {
        this.memberId = memberId;
        this.membershipId = membershipId;
        this.branchId = branchId;
        this.entryAt = entryAt;
        this.entryDate = entryAt.toLocalDate();
        this.deducted = deducted;
    }

    /**
     * 서버 시각으로 출입을 기록한다 (FR-3.1).
     * serverNow는 KST 시계에서 얻은 값이어야 한다. 응답 형식(04 §1)에 맞춰 초 단위로 저장한다.
     */
    public static AttendanceRecord record(
            Long memberId, Long membershipId, Long branchId, LocalDateTime serverNow, boolean deducted) {
        if (memberId == null || membershipId == null || branchId == null || serverNow == null) {
            throw new IllegalArgumentException("출입 기록의 회원 · 회원권 · 지점 · 시각은 필수입니다.");
        }
        return new AttendanceRecord(
                memberId, membershipId, branchId, serverNow.truncatedTo(ChronoUnit.SECONDS), deducted);
    }
}
