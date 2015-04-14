package org.sportim.service.ultimatefrisbee.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.beans.stats.AggregateEventBean;
import org.sportim.service.beans.stats.SportType;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class UltimateEventBean extends AggregateEventBean {
    public int totalPoints = 0;

    public UltimateEventBean(int eventID) {
        super(SportType.ULTIMATE_FRISBEE, eventID);
    }
}
