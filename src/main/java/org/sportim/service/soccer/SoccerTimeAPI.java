package org.sportim.service.soccer;

import org.apache.log4j.Logger;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.stats.AggregateEventBean;
import org.sportim.service.beans.stats.TeamStatsBean;
import org.sportim.service.soccer.beans.SoccerEventBean;
import org.sportim.service.soccer.beans.SoccerGameBean;
import org.sportim.service.soccer.beans.SoccerTeamStatsBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * API for recording game and player time.
 */
@Path("/time")
public class SoccerTimeAPI {
    private static Logger logger = Logger.getLogger(SoccerTimeAPI.class.getName());
    private ConnectionProvider provider;
    private SoccerAggregationAPI soccerStatAPI;

    public SoccerTimeAPI() {
        provider = ConnectionManager.getInstance();
        soccerStatAPI = new SoccerAggregationAPI(provider);
    }

    public SoccerTimeAPI(ConnectionProvider provider) {
        this.provider = provider;
        soccerStatAPI = new SoccerAggregationAPI(provider);
    }

    /**
     * Record the start of a game
     * @param gameStart body param, contains game info
     * @param eventID path param, the event ID
     * @param token header param, the user's authentication token
     * @param session header param, the user's stat tracking session ID
     * @return a ResponseBean containing the result status
     */
    @POST
    @Produces("application/json")
    @Path("start/{eventID}")
    public ResponseBean startGame(@PathParam("eventID") final int eventID, @HeaderParam("session") final String session,
                                  @HeaderParam("token") final String token, SoccerGameBean gameStart) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!StatUtil.isValidSession(session, eventID)) {
            return new ResponseBean(400, "You must start a session before tracking any statistics");
        }

        boolean success = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT INTO SoccerTime (eventID, start) VALUES (?,?) " +
                    "ON DUPLICATE KEY UPDATE start = ?");
            stmt.setInt(1, eventID);
            stmt.setLong(2, gameStart.getTimestampMillis());
            stmt.setLong(3, gameStart.getTimestampMillis());
            success = stmt.executeUpdate() > 0;

            // Add timestamp for all of the starters
            stmt = conn.prepareStatement("INSERT INTO SoccerStats (eventID, teamID, player, timeOn) VALUES (?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE timeOn = ?");
            addStarterBatch(stmt, eventID, gameStart.starters, gameStart.teamID, gameStart.getTimestampMillis());
            addStarterBatch(stmt, eventID, gameStart.starters2, gameStart.teamID2, gameStart.getTimestampMillis());
            stmt.executeBatch();
        } catch (Exception e) {
            logger.error("Unable to start soccer game: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            success = false;
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (success) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to start game");
    }

    /**
     * Record the end of the first half
     * @param halfEnd body param, contains half info
     * @param eventID path param, the event ID
     * @param token header param, the user's authentication token
     * @param session header param, the user's stat tracking session ID
     * @return a ResponseBean containing the result status
     */
    @POST
    @Produces("application/json")
    @Path("halfend/{eventID}")
    public ResponseBean endHalf(@PathParam("eventID") final int eventID, @HeaderParam("session") final String session,
                                @HeaderParam("token") final String token, SoccerGameBean halfEnd) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!StatUtil.isValidSession(session, eventID)) {
            return new ResponseBean(400, "You must start a session before tracking any statistics");
        }

        boolean success = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("UPDATE SoccerTime SET half_end = ? WHERE eventID = ?");
            stmt.setLong(1, halfEnd.getTimestampMillis());
            stmt.setInt(2, eventID);
            success = stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Unable to end soccer half: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            success = false;
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (success) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to end half. Make sure the game was started.");
    }

    /**
     * Record the start of the second half
     * @param halfStart body param, contains half info
     * @param eventID path param, the event ID
     * @param token header param, the user's authentication token
     * @param session header param, the user's stat tracking session ID
     * @return a ResponseBean containing the result status
     */
    @POST
    @Produces("application/json")
    @Path("halfstart/{eventID}")
    public ResponseBean startHalf(@PathParam("eventID") final int eventID, @HeaderParam("session") final String session,
                                @HeaderParam("token") final String token, SoccerGameBean halfStart) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!StatUtil.isValidSession(session, eventID)) {
            return new ResponseBean(400, "You must start a session before tracking any statistics");
        }

        boolean success = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("UPDATE SoccerTime SET half_start = ? WHERE eventID = ?");
            stmt.setLong(1, halfStart.getTimestampMillis());
            stmt.setInt(2, eventID);
            success = stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Unable to start soccer half: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            success = false;
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (success) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to start half. Make sure the game was started.");
    }

    private void addStarterBatch(PreparedStatement stmt, int eventID, List<String> players, int teamID, long time) throws SQLException {
        for (String login : players) {
            stmt.setInt(1, eventID);
            stmt.setInt(2, teamID);
            stmt.setString(3, login);
            stmt.setLong(4, time);
            stmt.setLong(5, time);
            stmt.addBatch();
        }
    }

    /**
     * Record the end of the game
     * @param gameEnd body param, contains game end info
     * @param eventID path param, the event ID
     * @param token header param, the user's authentication token
     * @param session header param, the user's stat tracking session ID
     * @return a ResponseBean containing the result status
     */
    @POST
    @Produces("application/json")
    @Path("end/{eventID}")
    public ResponseBean endGame(@PathParam("eventID") final int eventID, @HeaderParam("session") final String session,
                                  @HeaderParam("token") final String token, SoccerGameBean gameEnd) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!StatUtil.isValidSession(session, eventID)) {
            return new ResponseBean(400, "You must start a session before tracking any statistics");
        }

        boolean success = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT INTO SoccerTime (eventID, end) VALUES (?,?) " +
                    "ON DUPLICATE KEY UPDATE end = ?");
            stmt.setInt(1, eventID);
            stmt.setLong(2, gameEnd.getTimestampMillis());
            stmt.setLong(3, gameEnd.getTimestampMillis());
            success = stmt.executeUpdate() > 0;

            long half_end = 0, half_start = 0, end = 0;
            if (success) {
                // Update minutes played for all players - get the times of this game
                stmt = conn.prepareStatement("SELECT half_end, half_start, end FROM SoccerTime WHERE eventID = ?");
                stmt.setInt(1, eventID);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    half_end = rs.getLong(1);
                    half_start = rs.getLong(2);
                    end = rs.getLong(3);
                } else {
                    success = false;
                }
                APIUtils.closeResources(rs, stmt);
            }

            if (success) {
                // first get anybody with a timeOn and no minutes played (these were subbed on and not subbed off)
                stmt = conn.prepareStatement("SELECT teamID, player, timeOn, minutes, eventID FROM SoccerStats WHERE eventID = ? " +
                        "AND minutes = 0", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                stmt.setInt(1, eventID);
                rs = stmt.executeQuery();

                // Then calculate their minutes and stick them in
                while (rs.next()) {
                    long timeOn = rs.getLong(3);
                    int min = minutesPlayed(timeOn, half_end, half_start, end);
                    rs.updateInt(4, min);
                    rs.updateRow();
                }
            }

            Set<Integer> losers = new HashSet<Integer>();
            int winner = soccerStatAPI.getEventWinner(eventID, losers);
            if (winner != -1) {
                StatUtil.fillNextBracketEvent(eventID, winner, losers);
            }
        } catch (Exception e) {
            logger.error("Unable to end soccer game: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            success = false;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (success) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to end game");
    }

    /**
     * Record a substitution
     * @param sub body param, contains the sub's info
     * @param eventID path param, the event ID
     * @param token header param, the user's authentication token
     * @param session header param, the user's stat tracking session ID
     * @return a ResponseBean containing the result status
     */
    @POST
    @Produces("application/json")
    @Path("sub/{eventID}")
    public ResponseBean substitute(@PathParam("eventID") final int eventID, @HeaderParam("session") final String session,
                                   @HeaderParam("token") final String token, SoccerGameBean sub) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!StatUtil.isValidSession(session, eventID)) {
            return new ResponseBean(400, "You must start a session before tracking any statistics");
        }

        boolean success = true;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        long half_end = 0, half_start = 0;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT half_end, half_start FROM SoccerTime WHERE eventID = ?");
            stmt.setInt(1, eventID);
            rs = stmt.executeQuery();
            if (rs.next()) {
                half_end = rs.getLong(1);
                half_end = half_end > 0 ? half_end : sub.getTimestampMillis() + 1;
                half_start = rs.getLong(2);
                half_start = half_start > 0 ? half_start : sub.getTimestampMillis() + 1;
            } else {
                success = false;
            }
            APIUtils.closeResources(rs, stmt);

            // calculate minutes played for subbed off player
            stmt = conn.prepareStatement("SELECT teamID, timeOn, minutes, eventID, player FROM SoccerStats WHERE eventID = ? " +
                    "AND player = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setInt(1, eventID);
            stmt.setString(2, sub.subOff);
            rs = stmt.executeQuery();
            if (success && rs.next()) {
                long timeOn = rs.getLong(2);
                int min = minutesPlayed(timeOn, half_end, half_start, sub.getTimestampMillis());
                rs.updateInt(3, min);
                rs.updateRow();
            } else {
                success = false;
            }

            // Add time on for subbed on player
            stmt = conn.prepareStatement("INSERT INTO SoccerStats (eventID, player, teamID, timeOn) VALUES (?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE timeOn = ?");
            stmt.setInt(1, eventID);
            stmt.setString(2, sub.subOn);
            stmt.setInt(3, sub.teamID);
            stmt.setLong(4, sub.getTimestampMillis());
            stmt.setLong(5, sub.getTimestampMillis());
            success = stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Unable to post substitute: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            success = false;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (success) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to make substitution. Make sure the game has been started and one of the " +
                "players was already on the field.");
    }

    private int minutesPlayed(long timeOn, long half_end, long half_start, long end) {
        if (timeOn > half_start) {
            return millisToMinutes(end - timeOn);
        }
        return millisToMinutes(end - timeOn - (half_start - half_end));
    }

    private int millisToMinutes(long millis) {
        // downcasting's ok here - we'll be small; ~90 minutes tops
        return (int)(millis / 60000);
    }
}
