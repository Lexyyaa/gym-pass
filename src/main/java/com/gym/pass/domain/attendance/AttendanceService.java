package com.gym.pass.domain.attendance;

import com.gym.pass.domain.attendance.exception.AttendanceException;
import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.domain.member.MemberRepository;
import com.gym.pass.domain.membership.Membership;
import com.gym.pass.domain.membership.MembershipRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 출입 도메인 서비스 (FR-3.1 ~ FR-3.4 · NFR-2 · NFR-4 · 03 §5 · §7 · §8).
 * membership 행 락 → 유효 검사 → 오늘 첫 출입이면 차감 → 기록. 호출 측 트랜잭션 안에서 실행돼야 한다.
 */
@Component
@RequiredArgsConstructor
public class AttendanceService {

    private final MembershipRepository membershipRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final MemberRepository memberRepository;

    /** serverNow는 KST 시계의 현재 시각이다. 오늘 = serverNow의 달력일 (D-5). */
    public AttendanceRecord enter(Long memberId, Long branchId, LocalDateTime serverNow) {
        LocalDate today = serverNow.toLocalDate();
        // 락 조회가 트랜잭션의 첫 쿼리다. 이후 일반 조회의 스냅샷은 락을 잡은 뒤에 생긴다 (03 §7)
        Membership membership = membershipRepository
                .findValidByMemberIdForUpdate(memberId, today)
                .orElseThrow(() -> noValidMembership(memberId));
        membership.verifyBranch(branchId);
        membership.validateEntry(today);

        boolean deducted = false;
        if (!attendanceRecordRepository.existsDeductedOn(membership.getId(), today)) {
            deducted = membership.deduct(today);
        }
        return attendanceRecordRepository.save(
                AttendanceRecord.record(memberId, membership.getId(), membership.getBranchId(), serverNow, deducted));
    }

    /** 판정 대상이 없으면 회원 존재부터 확인한다. 없는 회원은 MEMBER_NOT_FOUND다. */
    private AttendanceException noValidMembership(Long memberId) {
        memberRepository.getById(memberId);
        return new AttendanceException(ErrorCode.ATTENDANCE_NO_VALID_MEMBERSHIP);
    }
}
