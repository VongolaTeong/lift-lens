package com.liftlens.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A single training session. Identified by {@code hevyNaturalKey} (hash of title + start time)
 * so overlapping re-exports upsert onto the same row instead of duplicating.
 */
@Entity
@Table(name = "workout")
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hevy_natural_key", nullable = false, unique = true)
    private String hevyNaturalKey;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_category", nullable = false)
    private SplitCategory splitCategory = SplitCategory.OTHER;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Workout() {
        // built field-by-field at ingest; also satisfies JPA
    }

    public Long getId() {
        return id;
    }

    public String getHevyNaturalKey() {
        return hevyNaturalKey;
    }

    public void setHevyNaturalKey(String hevyNaturalKey) {
        this.hevyNaturalKey = hevyNaturalKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public SplitCategory getSplitCategory() {
        return splitCategory;
    }

    public void setSplitCategory(SplitCategory splitCategory) {
        this.splitCategory = splitCategory;
    }
}
