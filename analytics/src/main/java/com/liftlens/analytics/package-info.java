/**
 * Pure analytics math for LiftLens — estimated 1RM, working volume, trend slopes, and the
 * bodyweight reps-progression series (CLAUDE.md §5).
 *
 * <p>This package has <strong>zero</strong> Spring/DB dependencies so the math is trivially
 * unit-testable; persistence and materialization live in the {@code api} module. Real
 * implementations land in Phase 2.
 */
package com.liftlens.analytics;
