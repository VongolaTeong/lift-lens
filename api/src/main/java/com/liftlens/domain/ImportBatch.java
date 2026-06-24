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

/** One ingestion event. The {@code checksum} (sha256 of the file) makes exact re-imports a no-op. */
@Entity
@Table(name = "import_batch")
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private ImportSource source;

    @Column(name = "filename")
    private String filename;

    @Column(name = "checksum", nullable = false, unique = true)
    private String checksum;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @CreationTimestamp
    @Column(name = "imported_at", nullable = false, updatable = false)
    private Instant importedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ImportStatus status = ImportStatus.PENDING;

    protected ImportBatch() {
        // for JPA
    }

    public ImportBatch(ImportSource source, String filename, String checksum, int rowCount) {
        this.source = source;
        this.filename = filename;
        this.checksum = checksum;
        this.rowCount = rowCount;
    }

    public void markCompleted() {
        this.status = ImportStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = ImportStatus.FAILED;
    }

    public Long getId() {
        return id;
    }

    public ImportSource getSource() {
        return source;
    }

    public String getFilename() {
        return filename;
    }

    public String getChecksum() {
        return checksum;
    }

    public int getRowCount() {
        return rowCount;
    }

    public ImportStatus getStatus() {
        return status;
    }
}
