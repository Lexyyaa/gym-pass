package com.gym.pass.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 단일 관리. docs/design/04-api-spec.md §4와 1:1로 맞춘다.
 * 새 코드는 문서에 먼저 추가하고, 도메인별 주석 블록 아래에 모은다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    COMMON_INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    COMMON_RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    COMMON_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    COMMON_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 지점
    BRANCH_NOT_FOUND(HttpStatus.NOT_FOUND, "지점을 찾을 수 없습니다."),
    BRANCH_FORBIDDEN(HttpStatus.FORBIDDEN, "다른 지점의 데이터에 접근할 수 없습니다."),

    // 회원
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    MEMBER_PHONE_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 연락처입니다."),
    MEMBER_HAS_RELATED_DATA(HttpStatus.CONFLICT, "연관 데이터가 있어 삭제할 수 없습니다."),

    // 회원권
    MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "회원권을 찾을 수 없습니다."),
    MEMBERSHIP_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 유효한 회원권이 있습니다."),
    MEMBERSHIP_NOT_PAUSABLE(HttpStatus.CONFLICT, "정지할 수 없는 회원권입니다."),
    MEMBERSHIP_NOT_CANCELABLE(HttpStatus.CONFLICT, "취소할 수 없는 회원권입니다."),

    // 정지
    PAUSE_NOT_FOUND(HttpStatus.NOT_FOUND, "정지 내역을 찾을 수 없습니다."),
    PAUSE_START_DATE_PAST(HttpStatus.BAD_REQUEST, "정지 시작일은 오늘 이후여야 합니다."),
    PAUSE_COUNT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "정지 가능 횟수를 초과했습니다."),
    PAUSE_DAYS_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "정지 가능 일수를 초과했습니다."),
    PAUSE_OVERLAPPED(HttpStatus.CONFLICT, "기존 정지와 기간이 겹칩니다."),
    PAUSE_NOT_RELEASABLE(HttpStatus.CONFLICT, "해제할 수 없는 정지입니다."),

    // 출입
    ATTENDANCE_NO_VALID_MEMBERSHIP(HttpStatus.CONFLICT, "유효한 회원권이 없습니다."),
    ATTENDANCE_MEMBERSHIP_PAUSED(HttpStatus.CONFLICT, "정지 중인 회원권입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
