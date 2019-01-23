package com.github.frankfarrell.jlambdadog;

public enum MetricType {
    COUNT("count"),
    GAUGE("gauge"),
    HISTOGRAM("histogram"),
    CHECK("check");

    private final String displayName;

    private MetricType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
