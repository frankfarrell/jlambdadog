package com.github.frankfarrell.jlambdadog;

import java.util.EnumSet;

public enum Expansion {

    COUNT("count"),
    MIN("min"),
    MEAN("mean"),
    MAX("max"),
    STD_DEV("stddev"),
    MEDIAN("median"),
    P75("p75"),
    P95("p95"),
    P98("p98"),
    P99("p99"),
    P999("p999");

    public static EnumSet<Expansion> ALL = EnumSet.allOf(Expansion.class);

    private final String displayName;

    private Expansion(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
