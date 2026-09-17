package com.gym.pass.infrastructure.persistence.branch;

import com.gym.pass.domain.branch.Branch;
import com.gym.pass.domain.branch.BranchRepository;
import com.gym.pass.domain.branch.exception.BranchException;
import com.gym.pass.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BranchRepositoryImpl implements BranchRepository {

    private final BranchJpaRepository branchJpaRepository;

    @Override
    public Branch getById(Long id) {
        return branchJpaRepository.findById(id).orElseThrow(() -> new BranchException(ErrorCode.BRANCH_NOT_FOUND));
    }
}
