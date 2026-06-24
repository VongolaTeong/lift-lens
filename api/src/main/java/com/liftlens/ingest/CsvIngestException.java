package com.liftlens.ingest;

/** Thrown when a CSV cannot be parsed (missing required columns, unreadable, bad value). */
public class CsvIngestException extends RuntimeException {

    public CsvIngestException(String message) {
        super(message);
    }

    public CsvIngestException(String message, Throwable cause) {
        super(message, cause);
    }
}
