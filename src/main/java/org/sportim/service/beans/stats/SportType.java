package org.sportim.service.beans.stats;

public enum SportType {
    SOCCER("Soccer"),
    ULTIMATE_FRISBEE("Ultimate Frisbee"),
    UNKNOWN("unknown");

    private final String type;

    private SportType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}