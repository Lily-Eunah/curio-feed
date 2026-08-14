package com.curiofeed.backend.infrastructure.llm.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class SpearmanCorrelationCalculatorTest {

    @Test
    @DisplayName("Perfect positive correlation returns 1.0")
    void calculate_perfectPositive_returnsOne() {
        double[] x = {0.5, 0.6, 0.7, 0.8, 0.9};
        double[] y = {0.55, 0.65, 0.75, 0.85, 0.95};

        double rs = SpearmanCorrelationCalculator.calculate(x, y);

        assertThat(rs).isCloseTo(1.0, offset(0.001));
    }

    @Test
    @DisplayName("Perfect negative correlation returns -1.0")
    void calculate_perfectNegative_returnsMinusOne() {
        double[] x = {0.1, 0.2, 0.3, 0.4, 0.5};
        double[] y = {0.5, 0.4, 0.3, 0.2, 0.1};

        double rs = SpearmanCorrelationCalculator.calculate(x, y);

        assertThat(rs).isCloseTo(-1.0, offset(0.001));
    }

    @Test
    @DisplayName("Tied ranks handle fractional average ranks correctly")
    void calculate_withTies_handlesTies() {
        double[] x = {0.5, 0.5, 0.7, 0.8};
        double[] y = {0.4, 0.6, 0.7, 0.8};

        double rs = SpearmanCorrelationCalculator.calculate(x, y);

        assertThat(rs).isGreaterThan(0.5);
    }
}
