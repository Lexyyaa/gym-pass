package com.gym.pass.infrastructure.persistence.membership;

import com.gym.pass.domain.membership.Membership;
import com.gym.pass.domain.membership.MembershipStatus;
import java.time.LocalDate;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipJpaRepository extends JpaRepository<Membership, Long> {

    boolean existsByMemberIdAndStatusInAndEndDateGreaterThanEqual(
            Long memberId, Collection<MembershipStatus> statuses, LocalDate today);
}
