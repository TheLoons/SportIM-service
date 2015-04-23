package org.sportim.service.util;

/**
 * Enum of supported sports for statistics plugins.
 */
public enum SportType {
    SOCCER("Soccer"),
    ULTIMATE_FRISBEE("Ultimate Frisbee"),
    UNKNOWN("Unknown");

    private final String type;

    private SportType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static SportType fromString(final String type) {
        if (type == null) {
            return SportType.UNKNOWN;
        }
        if (type.equalsIgnoreCase("soccer")) {
            return SportType.SOCCER;
        }
        if (type.equalsIgnoreCase("ultimate frisbee") || type.equalsIgnoreCase("ultimate_frisbee")) {
            return SportType.ULTIMATE_FRISBEE;
        }
        return SportType.UNKNOWN;
    }
}