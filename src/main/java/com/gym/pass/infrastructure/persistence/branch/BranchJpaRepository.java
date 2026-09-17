package com.gym.pass.infrastructure.persistence.branch;

import com.gym.pass.domain.branch.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchJpaRepository extends JpaRepository<Branch, Long> {}
