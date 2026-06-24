package com.liftlens.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class LinearRegressionTest {

    @Test
    void positiveSlopeForRisingSeries() {
        // y = 2x + 1
        double slope = LinearRegression.slope(new double[] {0, 1, 2, 3}, new double[] {1, 3, 5, 7});
        assertThat(slope).isCloseTo(2.0, within(1e-9));
    }

    @Test
    void negativeSlopeForFallingSeries() {
        double slope = LinearRegression.slope(new double[] {0, 1, 2}, new double[] {10, 8, 6});
        assertThat(slope).isCloseTo(-2.0, within(1e-9));
    }

    @Test
    void zeroSlopeForFlatSeries() {
        double slope = LinearRegression.slope(new double[] {0, 1, 2, 3}, new double[] {5, 5, 5, 5});
        assertThat(slope).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void nanForFewerThanTwoPoints() {
        assertThat(LinearRegression.slope(new double[] {1}, new double[] {2})).isNaN();
    }

    @Test
    void nanWhenXHasNoVariance() {
        assertThat(LinearRegression.slope(new double[] {3, 3, 3}, new double[] {1, 2, 3})).isNaN();
    }
}
