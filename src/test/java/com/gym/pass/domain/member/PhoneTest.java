package com.gym.pass.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneTest {

    @ParameterizedTest
    @ValueSource(strings = {"010-1234-5678", "011-123-4567"})
    @DisplayName("[TC-1-01] 올바른 형식의 연락처로 값 객체를 만들면 값이 보존된다")
    void create(String value) {
        // given
        // when
        Phone phone = Phone.of(value);

        // then
        assertThat(phone.getValue()).isEqualTo(value);
        assertThat(phone).isEqualTo(Phone.of(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "010-abcd-5678", "010-1234-567a", "01012345678", "02-1234-5678"})
    @DisplayName("[TC-1-02] 형식 오류 연락처(빈 값 · 문자 포함)로 만들면 IllegalArgumentException이 발생한다")
    void invalid(String value) {
        // given
        // when
        // then
        assertThatThrownBy(() -> Phone.of(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
