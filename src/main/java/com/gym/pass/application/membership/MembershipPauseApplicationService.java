package com.gym.pass.application.membership;

import com.gym.pass.domain.membership.MembershipPauseLimits;
import com.gym.pass.domain.membership.MembershipPauseService;
import com.gym.pass.support.properties.MembershipProperties;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 정지 등록 · 조기 해제 (API-4 · API-5).
 * 회원 id 조회는 락 트랜잭션 밖에서 하고, 락 → 검사 → 변경은 1트랜잭션이다 (D-26 · 03 §7).
 * 회원 id 조회를 같은 트랜잭션에 두면 스냅샷이 member 락보다 먼저 잡히므로 TransactionTemplate으로 경계를 나눈다.
 * 바깥 트랜잭션에 합류하면 그 스냅샷을 이어 쓰게 되므로 전파는 REQUIRES_NEW다.
 */
@Service
public class MembershipPauseApplicationService {

    private final MembershipPauseService membershipPauseService;
    private final MembershipProperties membershipProperties;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public MembershipPauseApplicationService(
            MembershipPauseService membershipPauseService,
            MembershipProperties membershipProperties,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.membershipPauseService = membershipPauseService;
        this.membershipProperties = membershipProperties;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 정지 등록 (FR-4.1 ~ FR-4.3). 상한은 설정값이다 (D-2). */
    public MembershipPauseInfo.Paused pause(MembershipPauseCommand.Pause command) {
        Long memberId = membershipPauseService.memberIdOf(command.membershipId());
        MembershipProperties.Pause pauseProperties = membershipProperties.pause();
        MembershipPauseLimits limits =
                new MembershipPauseLimits(pauseProperties.maxCount(), pauseProperties.daysPerMonth());
        return transactionTemplate.execute(status -> MembershipPauseInfo.Paused.from(membershipPauseService.pause(
                memberId,
                command.membershipId(),
                command.branchId(),
                command.startDate(),
                command.days(),
                LocalDate.now(clock),
                limits)));
    }

    /** 정지 조기 해제 (FR-4.2). */
    public MembershipPauseInfo.Released release(MembershipPauseCommand.Release command) {
        Long memberId = membershipPauseService.memberIdOf(command.membershipId());
        return transactionTemplate.execute(status -> MembershipPauseInfo.Released.from(membershipPauseService.release(
                memberId, command.membershipId(), command.branchId(), command.pauseId(), LocalDate.now(clock))));
    }
}
