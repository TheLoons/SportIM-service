package org.sportim.service.soccer;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.soccer.beans.GameBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * API for recording game and player time.
 */
@Path("/time")
public class SoccerTimeAPI {
    private ConnectionProvider provider;

    public SoccerTimeAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SoccerTimeAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @POST
    @Produces("application/json")
    @Path("start/{eventID}")
    public ResponseBean startGame(@PathParam("eventID") final int eventID, @HeaderParam("session") final String session,
                                  @HeaderParam("token") final String token, GameBean gameStart) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!SoccerUtil.isValidSession(session, eventID)) {
            return new ResponseBean(409, "Invalid session. Someone may have taken control of this statistics tracking session.");
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
            addStarterBatch(stmt, eventID, gameStart.starters1, gameStart.teamID1, gameStart.getTimestampMillis());
            addStarterBatch(stmt, eventID, gameStart.starters2, gameStart.teamID2, gameStart.getTimestampMillis());
            stmt.executeBatch();
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
            success = false;
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (success) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to start game");
    }

    private void addStarterBatch(PreparedStatement stmt, int eventID, List<String> players, int teamID, long time) throws SQLException {
        for (String login : players) {
            stmt.setInt(1, eventID);
            stmt.setInt(2, teamID);
            stmt.setString(3, login);
            stmt.setLong(4, time);
            stmt.addBatch();
        }
    }

    @POST
    @Produces("application/json")
    @Path("end/{eventID}")
    public ResponseBean endGame(@PathParam("eventID") final int eventID, @HeaderParam("session") final String session,
                                  @HeaderParam("token") final String token, GameBean gameEnd) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!SoccerUtil.isValidSession(session, eventID)) {
            return new ResponseBean(409, "Invalid session. Someone may have taken control of this statistics tracking session.");
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

            long start = 0, half_end = 0, half_start = 0, end = 0;
            if (success) {
                // Update minutes played for all players - get the times of this game
                stmt = conn.prepareStatement("SELECT 'start', 'half_end', 'half_start', 'end' FROM SoccerTime WHERE eventID = ?");
                stmt.setInt(1, eventID);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    start = rs.getLong(1);
                    half_end = rs.getLong(2);
                    half_start = rs.getLong(3);
                    end = rs.getLong(4);
                } else {
                    success = false;
                }
                APIUtils.closeResources(rs, stmt);
            }

            if (success) {
                // first get anybody with a timeOn and no minutes played (these were subbed on and not subbed off)
                stmt = conn.prepareStatement("SELECT teamID, player, timeOn, minutes FROM SoccerStats WHERE eventID = ? " +
                        "AND minutes = 0");
                stmt.setInt(1, eventID);
                rs = stmt.executeQuery();

                // Then calculate their minutes and stick them in
                while (rs.next()) {
                    long timeOn = rs.getLong(3);
                    int min = minutesPlayed(timeOn, start, half_end, half_start, end);
                    rs.updateInt(4, min);
                    rs.updateRow();
                }
            }
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
            success = false;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (success) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to end game");
    }

    public int minutesPlayed(long timeOn, long start, long half_end, long half_start, long end) {
        if (timeOn > half_start) {
            return millisToMinutes(end - timeOn);
        }
        return millisToMinutes(end - timeOn - (half_start - half_end));
    }

    public int millisToMinutes(long millis) {
        // downcasting's ok here - we'll be small; ~90 minutes tops
        return (int)(millis / 60000);
    }
}
