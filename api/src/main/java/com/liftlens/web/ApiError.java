package com.liftlens.web;

import java.time.Instant;

/** Uniform error body for the API: HTTP status, a short reason, and a human-readable message. */
public record ApiError(Instant timestamp, int status, String error, String message) {

    static ApiError of(org.springframework.http.HttpStatus status, String message) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message);
    }
}
