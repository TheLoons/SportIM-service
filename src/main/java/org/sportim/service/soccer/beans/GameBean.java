package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.util.APIUtils;

import java.util.List;

/**
 * Bean for game start/end information
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class GameBean {
    public int teamID;
    public List<String> starters;
    public int teamID2;
    public List<String> starters2;
    public String subOn;
    public String subOff;

    private String timestamp;
    private long timestampMillis;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        this.timestampMillis = APIUtils.parseDateTime(timestamp).getMillis();
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
