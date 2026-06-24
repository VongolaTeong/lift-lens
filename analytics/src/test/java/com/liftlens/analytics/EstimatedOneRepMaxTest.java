package com.liftlens.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class EstimatedOneRepMaxTest {

    @Test
    void epleyMatchesKnownValues() {
        // 100kg x 5 -> 100 * (1 + 5/30) = 116.6667
        assertThat(EstimatedOneRepMax.epley(100, 5)).isCloseTo(116.6667, within(1e-4));
        // a single rep returns the weight itself
        assertThat(EstimatedOneRepMax.epley(140, 1)).isCloseTo(144.6667, within(1e-4));
    }

    @Test
    void brzyckiMatchesKnownValues() {
        // 100kg x 5 -> 100 * 36 / (37 - 5) = 112.5
        assertThat(EstimatedOneRepMax.brzycki(100, 5)).isCloseTo(112.5, within(1e-6));
    }

    @Test
    void flagsReliableRepRange() {
        assertThat(EstimatedOneRepMax.isReliable(1)).isTrue();
        assertThat(EstimatedOneRepMax.isReliable(12)).isTrue();
        assertThat(EstimatedOneRepMax.isReliable(13)).isFalse();
        assertThat(EstimatedOneRepMax.isReliable(0)).isFalse();
    }

    @Test
    void rejectsNonPositiveReps() {
        assertThatThrownBy(() -> EstimatedOneRepMax.epley(100, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
