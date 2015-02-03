package org.sportim.service.soccer;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.soccer.beans.AggregateEventBean;
import org.sportim.service.soccer.beans.PlayerStatsBean;
import org.sportim.service.soccer.beans.ScoreBean;
import org.sportim.service.soccer.beans.TeamStatsBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * API for goal tracking
 */
@Path("/goal")
public class SoccerGoalAPI {
    private ConnectionProvider provider;

    public SoccerGoalAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SoccerGoalAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @POST
    @Path("{eventID}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean postGoal(final ScoreBean score, @PathParam("eventID") final int eventID,
                                 @HeaderParam("token") final String token, @HeaderParam("session") final String session) {
        if (!PrivilegeUtil.hasEventUpdate(token, eventID) || !SoccerUtil.isValidSession(session, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!score.validate()) {
            return new ResponseBean(400, "Malformed request");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT INTO SoccerStats (eventID, teamID, player, goals, shots, shotsongoal) VALUES (?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE goals = goals + 1, shots = shots + 1, shotsongoal = shotsongoal + 1");
            stmt.setInt(1, eventID);
            stmt.setInt(2, score.teamID);
            stmt.setString(3, score.player);
            stmt.setInt(4, 1);
            stmt.setInt(5, 1);
            stmt.setInt(6, 1);
            success = stmt.executeUpdate() > 0;

            APIUtils.closeResource(stmt);
            if (score.assist != null) {
                stmt = conn.prepareStatement("INSERT INTO SoccerStats (eventID, teamID, player, assists) VALUES (?,?,?) " +
                        "ON DUPLICATE KEY UPDATE assists = assists + 1");
                stmt.setInt(1, eventID);
                stmt.setInt(2, score.teamID);
                stmt.setString(3, score.assist);
                stmt.setInt(4, 1);
                success = success && (stmt.executeUpdate() > 0);
            }

            APIUtils.closeResource(stmt);
            stmt = conn.prepareStatement("INSERT INTO SoccerStats (eventID, teamID, player, goalsagainst) VALUES (?,?,?) " +
                    "ON DUPLICATE KEY UPDATE goalsagainst = goalsagainst + 1");
            stmt.setInt(1, eventID);
            stmt.setInt(2, score.goalieTeamID);
            stmt.setString(3, score.goalkeeper);
            stmt.setInt(4, 1);
            success = success && (stmt.executeUpdate() > 0);
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (success) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to add score");
    }

    @GET
    @Produces("application/json")
    @Path("{eventID}")
    public ResponseBean getEventGoals(@PathParam("eventID") final int eventID, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasEventView(token, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        AggregateEventBean eventStats = new AggregateEventBean();
        boolean success = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT teamID, SUM(goals), SUM(shots), SUM(shotsongoal), SUM(goalsagainst), " +
                    "SUM(fouls), SUM(yellow), SUM(red) FROM SoccerStats " +
                    "WHERE eventID = ? " +
                    "GROUP BY teamID");
            stmt.setInt(1, eventID);
            rs = stmt.executeQuery();
            eventStats.teamStats = new ArrayList<TeamStatsBean>(2);
            while(rs.next()) {
                TeamStatsBean teamStats = new TeamStatsBean();
                teamStats.teamID = rs.getInt(1);
                teamStats.goals = rs.getInt(2);
                teamStats.shots = rs.getInt(3);
                teamStats.shotsOnGoal = rs.getInt(4);
                teamStats.goalsAgainst = rs.getInt(5);
                teamStats.fouls = rs.getInt(6);
                teamStats.yellow = rs.getInt(7);
                teamStats.red = rs.getInt(8);
                eventStats.teamStats.add(teamStats);
            }

            APIUtils.closeResources(rs, stmt);
            for (TeamStatsBean team : eventStats.teamStats) {
                stmt = conn.prepareStatement("SELECT playerID, SUM(goals), SUM(shots), SUM(shotsongoal), SUM(goalsagainst), " +
                        "SUM(fouls), SUM(yellow), SUM(red) FROM SoccerStats " +
                        "WHERE eventID = ? AND teamID = ? " +
                        "GROUP BY playerID");
                stmt.setInt(1, eventID);
                stmt.setInt(2, team.teamID);
                rs = stmt.executeQuery();
                team.playerStats = new ArrayList<PlayerStatsBean>();
                while(rs.next()) {
                    PlayerStatsBean playerStats = new PlayerStatsBean();
                    playerStats.login = rs.getString(1);
                    playerStats.goals = rs.getInt(2);
                    playerStats.shots = rs.getInt(3);
                    playerStats.shotsOnGoal = rs.getInt(4);
                    playerStats.goalsAgainst = rs.getInt(5);
                    playerStats.fouls = rs.getInt(6);
                    playerStats.yellow = rs.getInt(7);
                    playerStats.red = rs.getInt(8);
                    team.playerStats.add(playerStats);
                }
                APIUtils.closeResources(rs, stmt);
            }
            success = true;
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
        return new ResponseBean(200, "");
    }
}
