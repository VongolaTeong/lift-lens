package com.liftlens.ingest;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

/**
 * Parses a Hevy CSV export into normalized {@link ParsedSetRow}s.
 *
 * <p>Stays general per CLAUDE.md §2: units are detected from the header (kg/km vs lbs/miles) and
 * normalized to kg + metres; dates use a non-padded day token with an explicit English locale.
 * The parser is pure (no Spring/DB state) so it is trivially unit-testable.
 */
@Component
public class HevyCsvParser {

    // "21 Jun 2026, 07:15" — day is NOT zero-padded, so use 'd' not 'dd'.
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private static final BigDecimal LBS_TO_KG = new BigDecimal("0.45359237");
    private static final BigDecimal MILES_TO_M = new BigDecimal("1609.344");
    private static final BigDecimal KM_TO_M = new BigDecimal("1000");

    private static final String[] REQUIRED = {
            "title", "start_time", "exercise_title", "set_index", "reps"};

    public List<ParsedSetRow> parse(byte[] content) {
        String text = stripBom(new String(content, StandardCharsets.UTF_8));
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        try (CSVParser parser = CSVParser.parse(new StringReader(text), format)) {
            List<String> headers = parser.getHeaderNames();
            requireColumns(headers);

            String weightColumn = pickRequired(headers, "weight_kg", "weight_lbs", "weight");
            String distanceColumn = pickOptional(headers, "distance_km", "distance_miles");
            boolean weightInLbs = "weight_lbs".equals(weightColumn);
            boolean distanceInMiles = "distance_miles".equals(distanceColumn);

            List<ParsedSetRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                rows.add(toRow(record, weightColumn, distanceColumn, weightInLbs, distanceInMiles));
            }
            return rows;
        } catch (IOException e) {
            throw new CsvIngestException("Could not read CSV", e);
        }
    }

    private ParsedSetRow toRow(CSVRecord record, String weightColumn, String distanceColumn,
            boolean weightInLbs, boolean distanceInMiles) {
        BigDecimal weight = decimal(get(record, weightColumn));
        if (weight != null && weightInLbs) {
            weight = weight.multiply(LBS_TO_KG).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal distance = decimal(get(record, distanceColumn));
        if (distance != null) {
            distance = distance.multiply(distanceInMiles ? MILES_TO_M : KM_TO_M)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        String setType = get(record, "set_type");
        return new ParsedSetRow(
                get(record, "title"),
                timestamp(get(record, "start_time"), true),
                timestamp(get(record, "end_time"), false),
                get(record, "description"),
                get(record, "exercise_title"),
                get(record, "superset_id"),
                get(record, "exercise_notes"),
                integer(get(record, "set_index")),
                setType == null ? "normal" : setType,
                weight,
                nullableInteger(get(record, "reps")),
                distance,
                nullableInteger(get(record, "duration_seconds")),
                decimal(get(record, "rpe")));
    }

    // ---- helpers ----------------------------------------------------------

    /** Returns the trimmed cell value, or null when the column is absent or the cell is blank. */
    private static String get(CSVRecord record, String column) {
        if (column == null || !record.isMapped(column)) {
            return null;
        }
        String value = record.get(column);
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private LocalDateTime timestamp(String value, boolean required) {
        if (value == null) {
            if (required) {
                throw new CsvIngestException("Missing required start_time");
            }
            return null;
        }
        try {
            return LocalDateTime.parse(value, TIMESTAMP);
        } catch (DateTimeParseException e) {
            throw new CsvIngestException("Unparseable timestamp: '" + value + "'", e);
        }
    }

    private static int integer(String value) {
        if (value == null) {
            throw new CsvIngestException("Missing required integer value");
        }
        return Integer.parseInt(value);
    }

    private static Integer nullableInteger(String value) {
        return value == null ? null : Integer.valueOf(value);
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private void requireColumns(List<String> headers) {
        for (String required : REQUIRED) {
            if (!headers.contains(required)) {
                throw new CsvIngestException("CSV is missing required column: " + required
                        + " (found: " + headers + ")");
            }
        }
    }

    private static String pickRequired(List<String> headers, String... candidates) {
        String found = pickOptional(headers, candidates);
        if (found == null) {
            throw new CsvIngestException("CSV is missing a weight column (one of: "
                    + String.join(", ", candidates) + ")");
        }
        return found;
    }

    private static String pickOptional(List<String> headers, String... candidates) {
        for (String candidate : candidates) {
            if (headers.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String stripBom(String text) {
        return (!text.isEmpty() && text.charAt(0) == '﻿') ? text.substring(1) : text;
    }
}
