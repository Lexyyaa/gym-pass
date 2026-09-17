package com.gym.pass.domain.branch;

public interface BranchRepository {

    /** 없으면 BRANCH_NOT_FOUND. */
    Branch getById(Long id);
}
