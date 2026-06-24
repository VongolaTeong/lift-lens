package com.liftlens.stats;

import java.util.Collection;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flags {@code exercise_set.is_pr} (CLAUDE.md §5/§6 {@code prScanJob}). A working set is a PR when it
 * sets a new running maximum for its exercise, ordered by time:
 * <ul>
 *   <li>weighted sets: new max weight, e1RM, or volume;</li>
 *   <li>bodyweight sets: new max reps.</li>
 * </ul>
 *
 * <p>The running max is computed with a window function over all strictly-prior working sets, so a
 * re-scan is deterministic and idempotent.
 */
@Service
public class PrScanService {

    private static final String SCAN_SQL = """
            WITH ordered AS (
                SELECT es.id, es.exercise_id, es.load_basis, es.weight_kg, es.reps,
                       w.started_at, es.set_index,
                       CASE WHEN es.weight_kg IS NOT NULL AND es.reps IS NOT NULL AND es.reps <= 12
                            THEN es.weight_kg * (1 + es.reps / 30.0) END AS e1rm,
                       CASE WHEN es.weight_kg IS NOT NULL AND es.reps IS NOT NULL
                            THEN es.weight_kg * es.reps END AS vol
                FROM exercise_set es
                JOIN workout w ON w.id = es.workout_id
                WHERE es.is_working AND es.exercise_id IN (:ids)
            ),
            ranked AS (
                SELECT id, load_basis, weight_kg, reps, e1rm, vol,
                       MAX(weight_kg) OVER pre AS prior_w,
                       MAX(e1rm)      OVER pre AS prior_e,
                       MAX(vol)       OVER pre AS prior_v,
                       MAX(reps)      OVER pre AS prior_r
                FROM ordered
                WINDOW pre AS (PARTITION BY exercise_id ORDER BY started_at, set_index
                               ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING)
            )
            UPDATE exercise_set t
            SET is_pr = CASE
                WHEN r.load_basis = 'WEIGHTED' THEN
                    (r.weight_kg IS NOT NULL AND (r.prior_w IS NULL OR r.weight_kg > r.prior_w))
                    OR (r.e1rm IS NOT NULL AND (r.prior_e IS NULL OR r.e1rm > r.prior_e))
                    OR (r.vol  IS NOT NULL AND (r.prior_v IS NULL OR r.vol  > r.prior_v))
                ELSE
                    (r.reps IS NOT NULL AND (r.prior_r IS NULL OR r.reps > r.prior_r))
                END
            FROM ranked r
            WHERE t.id = r.id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public PrScanService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void scan(Collection<Long> exerciseIds) {
        if (exerciseIds == null || exerciseIds.isEmpty()) {
            return;
        }
        jdbc.update(SCAN_SQL, Map.of("ids", exerciseIds));
    }
}
