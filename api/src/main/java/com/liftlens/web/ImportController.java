package com.liftlens.web;

import com.liftlens.domain.ImportSource;
import com.liftlens.ingest.ImportService;
import com.liftlens.ingest.ImportSummary;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Upload a Hevy CSV export and ingest it. Returns a batch summary (CLAUDE.md §7). */
@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportSummary upload(@RequestParam("file") MultipartFile file) throws IOException {
        return importService.importCsv(file.getOriginalFilename(), file.getBytes(), ImportSource.CSV);
    }
}
