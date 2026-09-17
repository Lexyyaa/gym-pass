package com.gym.pass.domain.member;

import com.gym.pass.domain.common.BaseTimeEntity;
import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.domain.member.exception.MemberException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 회원. 전사 공유, 지점 소속 없음 (03 §3.2 · D-10). */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    private static final int NAME_MAX_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Embedded
    private Phone phone;

    private Member(String name, Phone phone) {
        this.name = name;
        this.phone = phone;
    }

    public static Member create(String name, String phone) {
        if (name == null || name.isBlank() || name.length() > NAME_MAX_LENGTH) {
            throw new MemberException(ErrorCode.MEMBER_INVALID_INPUT, "이름은 1~50자여야 합니다.");
        }
        if (!Phone.isValid(phone)) {
            throw new MemberException(ErrorCode.MEMBER_INVALID_INPUT, "연락처 형식이 올바르지 않습니다.");
        }
        return new Member(name, Phone.of(phone));
    }
}
