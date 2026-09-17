package com.gym.pass.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.domain.member.exception.MemberException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MemberCreateTest {

    @Test
    @DisplayName("이름과 형식에 맞는 연락처로 회원을 만든다")
    void create() {
        // given
        // when
        Member member = Member.create("김지영", "010-1234-5678");

        // then
        assertThat(member.getName()).isEqualTo("김지영");
        assertThat(member.getPhone()).isEqualTo(Phone.of("010-1234-5678"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("[TC-2-12] 공백 이름으로 Member.create 하면 MEMBER_INVALID_INPUT 예외가 발생한다")
    void blankName(String name) {
        // given
        // when
        // then
        assertThatThrownBy(() -> Member.create(name, "010-1234-5678"))
                .isInstanceOf(MemberException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_INVALID_INPUT);
    }

    @Test
    @DisplayName("[TC-2-12] 50자를 넘는 이름으로 Member.create 하면 MEMBER_INVALID_INPUT 예외가 발생한다")
    void tooLongName() {
        // given
        String name = "가".repeat(51);

        // when
        // then
        assertThatThrownBy(() -> Member.create(name, "010-1234-5678"))
                .isInstanceOf(MemberException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_INVALID_INPUT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"01012345678", "02-123-4567", "010-12-5678", "010-1234-567a"})
    @DisplayName("[TC-2-12] 형식 오류 연락처로 Member.create 하면 IllegalArgumentException이 아니라 MEMBER_INVALID_INPUT 예외가 발생한다")
    void invalidPhone(String phone) {
        // given
        // when
        // then
        assertThatThrownBy(() -> Member.create("김지영", phone))
                .isInstanceOf(MemberException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_INVALID_INPUT);
    }
}
