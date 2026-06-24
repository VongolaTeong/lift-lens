package com.liftlens.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import com.liftlens.domain.SplitCategory;
import java.util.List;
import org.junit.jupiter.api.Test;

class SplitClassifierTest {

    @Test
    void legDayIsLower() {
        assertThat(SplitClassifier.classify(List.of("QUADS", "HAMSTRINGS", "GLUTES")))
                .isEqualTo(SplitCategory.LOWER);
    }

    @Test
    void balancedPushAndPullIsUpper() {
        // "Push pull" sessions are genuinely mixed -> UPPER, derived from muscles not the title.
        assertThat(SplitClassifier.classify(List.of("CHEST", "SHOULDERS", "BACK", "BICEPS")))
                .isEqualTo(SplitCategory.UPPER);
    }

    @Test
    void pushDominantIsPush() {
        assertThat(SplitClassifier.classify(List.of("CHEST", "SHOULDERS", "TRICEPS", "BACK")))
                .isEqualTo(SplitCategory.PUSH);
    }

    @Test
    void pullDominantIsPull() {
        assertThat(SplitClassifier.classify(List.of("BACK", "BICEPS", "REAR_DELTS", "CHEST")))
                .isEqualTo(SplitCategory.PULL);
    }

    @Test
    void coreOnlyIsOther() {
        assertThat(SplitClassifier.classify(List.of("ABS"))).isEqualTo(SplitCategory.OTHER);
    }

    @Test
    void emptyOrUnknownIsOther() {
        assertThat(SplitClassifier.classify(List.of())).isEqualTo(SplitCategory.OTHER);
        assertThat(SplitClassifier.classify(List.of("UNKNOWN"))).isEqualTo(SplitCategory.OTHER);
    }
}
