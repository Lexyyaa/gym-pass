package com.gym.pass.support.properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.gym.pass.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 회원권 정책 값이 test 프로파일에서도 application.yml 기본값으로 잡히는지 (C-34). */
@IntegrationTest
class MembershipPropertiesTest {

    @Autowired
    private MembershipProperties membershipProperties;

    @Test
    @DisplayName("[TC-2-14] test 프로파일에서 회원권 입력 상한은 120개월 · 1000회로 바인딩된다")
    void boundFromApplicationYml() {
        // given
        // when
        int maxMonths = membershipProperties.maxMonths();
        int maxCount = membershipProperties.maxCount();

        // then
        assertThat(maxMonths).isEqualTo(120);
        assertThat(maxCount).isEqualTo(1000);
    }
}
