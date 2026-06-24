package com.liftlens.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class HevyCsvParserTest {

    private static final String HEADER_KG =
            "title,start_time,end_time,description,exercise_title,superset_id,exercise_notes,"
                    + "set_index,set_type,weight_kg,reps,distance_km,duration_seconds,rpe";
    private static final String HEADER_LBS =
            "title,start_time,end_time,description,exercise_title,superset_id,exercise_notes,"
                    + "set_index,set_type,weight_lbs,reps,distance_miles,duration_seconds,rpe";

    private final HevyCsvParser parser = new HevyCsvParser();

    private static byte[] csv(String... lines) {
        return String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void parsesWeightedSetWithNonPaddedDate() {
        List<ParsedSetRow> rows = parser.parse(csv(HEADER_KG,
                "Lower,\"1 Feb 2026, 07:11\",\"1 Feb 2026, 08:00\",,\"Squat (Barbell)\",,,0,normal,100,5,,,"));

        assertThat(rows).hasSize(1);
        ParsedSetRow row = rows.get(0);
        assertThat(row.workoutTitle()).isEqualTo("Lower");
        assertThat(row.startTime()).isEqualTo(LocalDateTime.of(2026, 2, 1, 7, 11));
        assertThat(row.endTime()).isEqualTo(LocalDateTime.of(2026, 2, 1, 8, 0));
        assertThat(row.exerciseTitle()).isEqualTo("Squat (Barbell)");
        assertThat(row.setIndex()).isZero();
        assertThat(row.weightKg()).isEqualByComparingTo("100");
        assertThat(row.reps()).isEqualTo(5);
    }

    @Test
    void keepsBodyweightWeightNull() {
        List<ParsedSetRow> rows = parser.parse(csv(HEADER_KG,
                "Upper,\"1 Feb 2026, 07:11\",\"1 Feb 2026, 08:00\",,\"Pull Up\",,,0,normal,,8,,,"));

        ParsedSetRow row = rows.get(0);
        assertThat(row.weightKg()).isNull();
        assertThat(row.reps()).isEqualTo(8);
    }

    @Test
    void detectsLbsHeaderAndNormalizesToKg() {
        List<ParsedSetRow> rows = parser.parse(csv(HEADER_LBS,
                "Lower,\"1 Feb 2026, 07:11\",\"1 Feb 2026, 08:00\",,\"Bench Press (Barbell)\",,,0,normal,100,5,,,"));

        // 100 lbs * 0.45359237 = 45.359237 -> 45.36 kg
        assertThat(rows.get(0).weightKg()).isEqualByComparingTo("45.36");
    }

    @Test
    void normalizesDistanceKmToMetres() {
        List<ParsedSetRow> rows = parser.parse(csv(HEADER_KG,
                "Cardio,\"1 Feb 2026, 07:11\",\"1 Feb 2026, 08:00\",,\"Run\",,,0,normal,,,5,1800,"));

        ParsedSetRow row = rows.get(0);
        assertThat(row.distanceM()).isEqualByComparingTo("5000");
        assertThat(row.durationSeconds()).isEqualTo(1800);
        assertThat(row.reps()).isNull();
    }

    @Test
    void defaultsBlankSetTypeToNormal() {
        List<ParsedSetRow> rows = parser.parse(csv(HEADER_KG,
                "Lower,\"1 Feb 2026, 07:11\",\"1 Feb 2026, 08:00\",,\"Squat (Barbell)\",,,0,,100,5,,,"));

        assertThat(rows.get(0).setType()).isEqualTo("normal");
    }

    @Test
    void rejectsCsvMissingRequiredColumn() {
        assertThatThrownBy(() -> parser.parse(csv("title,start_time", "Lower,\"1 Feb 2026, 07:11\"")))
                .isInstanceOf(CsvIngestException.class)
                .hasMessageContaining("exercise_title");
    }
}
