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