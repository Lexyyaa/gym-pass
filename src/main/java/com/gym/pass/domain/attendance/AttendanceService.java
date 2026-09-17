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
 * member 행 락 → 후보 조회 → membership 행 락 → 유효 검사 → 오늘 첫 출입이면 차감 → 기록 (D-26).
 * 호출 측 트랜잭션 안에서 실행돼야 한다.
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
        // D-26 1번: member 행 락이 트랜잭션의 첫 쿼리다. 이후 일반 조회의 스냅샷은 락을 잡은 뒤에 생긴다 (03 §7)
        memberRepository.getByIdForUpdate(memberId);
        Long membershipId = membershipRepository
                .findValidIdByMemberId(memberId, today)
                .orElseThrow(AttendanceService::noValidMembership);
        // D-26 2번: 잠금 읽기는 최신 커밋 값을 읽는다. 락 대기 중 바뀌었을 수 있어 조건을 다시 검증한다
        Membership membership = membershipRepository.getByIdForUpdate(membershipId);
        if (!membership.isEntryCandidateOn(today)) {
            throw noValidMembership();
        }
        membership.verifyBranch(branchId);
        membership.validateEntry(today);

        boolean deducted = false;
        if (!attendanceRecordRepository.existsDeductedOn(membership.getId(), today)) {
            deducted = membership.deduct(today);
        }
        return attendanceRecordRepository.save(
                AttendanceRecord.record(memberId, membership.getId(), membership.getBranchId(), serverNow, deducted));
    }

    private static AttendanceException noValidMembership() {
        return new AttendanceException(ErrorCode.ATTENDANCE_NO_VALID_MEMBERSHIP);
    }
}
