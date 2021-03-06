package org.sportim.service.util;

import org.apache.log4j.Logger;
import org.sportim.service.api.AggregationAPI;
import org.sportim.service.beans.stats.AggregateEventBean;
import org.sportim.service.beans.stats.TeamStatsBean;
import org.sportim.service.soccer.beans.SoccerEventBean;
import org.sportim.service.soccer.beans.SoccerTeamStatsBean;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Utilities for the stats tracking APIs.
 */
public class StatUtil {
    private static Logger logger = Logger.getLogger(StatUtil.class.getName());
    private static ConnectionProvider provider = ConnectionManager.getInstance();

    public static void setConnectionProvider(ConnectionProvider provider) {
        StatUtil.provider = provider;
    }

    /**
     * Check if the given session is valid for this event
     * @param sessionID the session id
     * @param eventID the event id
     * @return true if the session is valid
     */
    public static boolean isValidSession(String sessionID, int eventID) {
        if (sessionID == null || sessionID.isEmpty() || eventID < 1) {
            return false;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean valid = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT COUNT(sessionID) FROM StatSessions " +
                    "WHERE eventID = ? and sessionID = ?");
            stmt.setInt(1, eventID);
            stmt.setString(2, sessionID);
            rs = stmt.executeQuery();
            valid = rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            logger.error("Unable to check session validity: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
        return valid;
    }

    /**
     * Find all of the teams in a given league
     * @param leagueID the league ID
     * @return a list of team IDs that are in the league
     */
    public static List<Integer> getAllTeamsInLeague(int leagueID) {
        List<Integer> teams = new LinkedList<Integer>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT TeamId FROM TeamBelongsTo WHERE LeagueId = ?");
            stmt.setInt(1, leagueID);
            rs = stmt.executeQuery();
            while (rs.next()) {
                teams.add(rs.getInt(1));
            }
        } catch (Exception e) {
            logger.error("Unable to get teams in league: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            return null;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        return teams;
    }

    /**
     * Add the winner of a match to the required participants in the next event
     * in the bracket, if necessary.
     *
     * @param eventID the event ID
     * @param winnerID the team ID of the winning team
     * @param losers list of any other team IDs in the match
     * @return true if the next bracket was filled
     */
    public static boolean fillNextBracketEvent(int eventID, int winnerID, Set<Integer> losers) {
        // Get the next bracket ID if any
        int nextEventID = getNextEventID(eventID);
        if (nextEventID < 1) {
            return false;
        }

        if (!removeTeams(nextEventID, losers)) {
            return false;
        }

        if (!addTeamToEvent(nextEventID, winnerID)) {
            return false;
        }

        return true;
    }

    /**
     * Get the next event ID in a bracket
     * @param eventID the current event ID
     * @return the ID of the next event that follows in a bracket, or 0 if no next event found
     */
    private static int getNextEventID(int eventID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int nextEventID = 0;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT NextEventId FROM Event WHERE EventId = ?");
            stmt.setInt(1, eventID);
            rs = stmt.executeQuery();
            if (rs.next()) {
                nextEventID = rs.getInt(1);
            }
        } catch (Exception e) {
            logger.error("Unable to get next event ID: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        return nextEventID;
    }

    /**
     * Add a team as a required participant to an event.
     *
     * @param eventID the event ID
     * @param teamID the team ID
     * @return true if successful, false otherwise
     */
    private static boolean addTeamToEvent(int eventID, int teamID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        int res = 0;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT IGNORE INTO TeamEvent (EventId, TeamId) VALUES (?,?)");
            stmt.setInt(1, eventID);
            stmt.setInt(2, teamID);
            res = stmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Unable to add team to event: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            return false;
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        return res > 0;
    }

    /**
     * Remove teams as required participants for an event
     * @param eventID the event ID
     * @param teamIDs the list of team IDs to remove
     * @return true if successful, false otherwise
     */
    private static boolean removeTeams(int eventID, Set<Integer> teamIDs) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("DELETE FROM TeamEvent WHERE EventId = ? AND TeamId = ?");
            for (Integer id : teamIDs) {
                stmt.setInt(1, eventID);
                stmt.setInt(2, id);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (Exception e) {
            logger.error("Unable to remove teams from event: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            return false;
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        return true;
    }
}
