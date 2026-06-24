package com.liftlens.repository;

import com.liftlens.domain.ImportBatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    Optional<ImportBatch> findByChecksum(String checksum);

    boolean existsByChecksum(String checksum);
}
