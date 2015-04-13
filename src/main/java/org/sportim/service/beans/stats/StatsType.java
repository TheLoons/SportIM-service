package org.sportim.service.beans.stats;

public enum StatsType {
    SOCCER("soccer"),
    ULTIMATE_FRISBEE("ultimate frisbee"),
    UNKNOWN("unknown");

    private final String type;

    private StatsType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}