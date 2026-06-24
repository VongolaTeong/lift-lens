package com.liftlens.domain;

/** Whether an exercise is multi-joint or single-joint. Matches the {@code movement_type} CHECK. */
public enum MovementType {
    COMPOUND,
    ISOLATION,
    UNKNOWN
}
