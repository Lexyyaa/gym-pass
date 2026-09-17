package com.gym.pass.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 연락처 값 객체. 형식: 01X-XXX(X)-XXXX (04 API-1 패턴과 같다).
 * 생성자 검증은 최후 방어선이다 — 사용자 입력은 Request 검증에서 먼저 막는다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Phone {

    private static final Pattern FORMAT = Pattern.compile("^01\\d-\\d{3,4}-\\d{4}$");

    @Column(name = "phone", nullable = false)
    private String value;

    private Phone(String value) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("연락처 형식이 올바르지 않습니다: " + value);
        }
        this.value = value;
    }

    public static Phone of(String value) {
        return new Phone(value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Phone other && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
