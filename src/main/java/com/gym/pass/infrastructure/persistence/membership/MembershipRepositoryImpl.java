package com.gym.pass.infrastructure.persistence.membership;

import com.gym.pass.domain.membership.Membership;
import com.gym.pass.domain.membership.MembershipRepository;
import com.gym.pass.domain.membership.MembershipStatus;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MembershipRepositoryImpl implements MembershipRepository {

    private static final List<MembershipStatus> VALID_STATUSES =
            List.of(MembershipStatus.ACTIVE, MembershipStatus.PAUSED);

    private final MembershipJpaRepository membershipJpaRepository;

    @Override
    public Membership save(Membership membership) {
        return membershipJpaRepository.save(membership);
    }

    @Override
    public boolean existsValidByMemberId(Long memberId, LocalDate today) {
        return membershipJpaRepository.existsByMemberIdAndStatusInAndEndDateGreaterThanEqual(
                memberId, VALID_STATUSES, today);
    }
}
