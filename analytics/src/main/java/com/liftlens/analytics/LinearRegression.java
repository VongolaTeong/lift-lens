package com.liftlens.analytics;

/**
 * Ordinary least-squares slope of y over x (CLAUDE.md §5 trend slope). Pure; matches Postgres
 * {@code regr_slope(y, x)} semantics so the in-DB materialization and any in-JVM detector agree.
 */
public final class LinearRegression {

    private LinearRegression() {
    }

    /**
     * @return the least-squares slope, or {@link Double#NaN} when there are fewer than two points
     *     or x has zero variance (a vertical/undefined fit).
     */
    public static double slope(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("x and y must be the same length");
        }
        int n = x.length;
        if (n < 2) {
            return Double.NaN;
        }
        double sumX = 0;
        double sumY = 0;
        double sumXX = 0;
        double sumXY = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXX += x[i] * x[i];
            sumXY += x[i] * y[i];
        }
        double denominator = n * sumXX - sumX * sumX;
        if (denominator == 0) {
            return Double.NaN;
        }
        return (n * sumXY - sumX * sumY) / denominator;
    }
}
