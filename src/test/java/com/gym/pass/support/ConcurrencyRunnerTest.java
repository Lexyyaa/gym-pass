package com.gym.pass.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConcurrencyRunnerTest {

    @Test
    @DisplayName("모든 스레드의 결과를 순서대로 모으고 성공과 실패를 구분한다")
    void collectsResults() throws InterruptedException {
        // given
        AtomicInteger counter = new AtomicInteger();

        // when
        List<ConcurrencyRunner.Result<Integer>> results = ConcurrencyRunner.run(10, index -> {
            counter.incrementAndGet();
            if (index % 2 == 0) {
                throw new IllegalStateException("fail " + index);
            }
            return index;
        });

        // then
        assertThat(counter.get()).isEqualTo(10);
        assertThat(results).hasSize(10);
        assertThat(ConcurrencyRunner.successCount(results)).isEqualTo(5);
        assertThat(ConcurrencyRunner.errors(results)).hasSize(5).allMatch(IllegalStateException.class::isInstance);
        assertThat(results.get(1).value()).isEqualTo(1);
    }
}
