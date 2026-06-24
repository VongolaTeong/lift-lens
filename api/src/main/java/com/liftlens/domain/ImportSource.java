package com.liftlens.domain;

/** Origin of an import batch. CSV is the v1 path; API is the optional Phase 7 sync source. */
public enum ImportSource {
    CSV,
    API
}
