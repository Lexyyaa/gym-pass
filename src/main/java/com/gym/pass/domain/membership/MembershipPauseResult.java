package com.gym.pass.domain.membership;

/** 정지 등록 · 해제 결과. 응답에 회원권 종료일과 정지 내용을 함께 싣는다. */
public record MembershipPauseResult(Membership membership, MembershipPause pause) {}
