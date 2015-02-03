package org.sportim.service.soccer;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.soccer.beans.AggregateEventBean;
import org.sportim.service.soccer.beans.PlayerStatsBean;
import org.sportim.service.soccer.beans.TeamStatsBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * API for getting aggregated stats
 */
@Path("stats")
public class SoccerAggregationAPI {
    private ConnectionProvider provider;

    public SoccerAggregationAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SoccerAggregationAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @GET
    @Produces("application/json")
    @Path("event/{eventID}")
    public ResponseBean getEventStats(@PathParam("eventID") final int eventID, @HeaderParam("token") final String token) {
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

                eventStats.totalGoals += teamStats.goals;
                eventStats.totalShots += teamStats.shots;
                eventStats.totalShotsOnGoal += teamStats.shotsOnGoal;
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

        if (success) {
            ResponseBean resp = new ResponseBean(200, "");
            resp.setEventStats(eventStats);
            return resp;
        }
        return new ResponseBean(500, "Unable to retrieve statistics.");
    }

    @GET
    @Produces("application/json")
    @Path("player")
    public ResponseBean getPlayerStats(@QueryParam("login") final String login, @QueryParam("teamID") final int teamID,
                                       @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasUserView(token, login)) {
            return new ResponseBean(401, "Not authorized");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        PlayerStatsBean playerStats = new PlayerStatsBean();
        boolean success = false;
        try {
            conn = provider.getConnection();
            if (teamID < 1) {
                stmt = conn.prepareStatement("SELECT playerID, SUM(goals), SUM(shots), SUM(shotsongoal), SUM(goalsagainst), " +
                        "SUM(fouls), SUM(yellow), SUM(red) FROM SoccerStats " +
                        "WHERE playerID = ?" +
                        "GROUP BY playerID");
                stmt.setString(1, login);
            } else {
                stmt = conn.prepareStatement("SELECT playerID, SUM(goals), SUM(shots), SUM(shotsongoal), SUM(goalsagainst), " +
                        "SUM(fouls), SUM(yellow), SUM(red) FROM SoccerStats " +
                        "WHERE playerID = ? AND teamID = ? " +
                        "GROUP BY playerID");
                stmt.setString(1, login);
                stmt.setInt(2, teamID);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                playerStats.login = rs.getString(1);
                playerStats.goals = rs.getInt(2);
                playerStats.shots = rs.getInt(3);
                playerStats.shotsOnGoal = rs.getInt(4);
                playerStats.goalsAgainst = rs.getInt(5);
                playerStats.fouls = rs.getInt(6);
                playerStats.yellow = rs.getInt(7);
                playerStats.red = rs.getInt(8);
            }
            success = true;
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (success) {
            ResponseBean resp = new ResponseBean(200, "");
            resp.setPlayerStats(playerStats);
            return resp;
        }
        return new ResponseBean(500, "Unable to retrieve statistics.");
    }

    @GET
    @Produces("application/json")
    @Path("team/{teamID}")
    public ResponseBean getTeamsStats(@PathParam("teamID") final int teamID, @HeaderParam("token") final String token) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        TeamStatsBean teamStats = new TeamStatsBean();
        boolean success = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT teamID, SUM(goals), SUM(shots), SUM(shotsongoal), SUM(goalsagainst), " +
                    "SUM(fouls), SUM(yellow), SUM(red) FROM SoccerStats " +
                    "WHERE teamID = ?" +
                    "GROUP BY teamID");
            stmt.setInt(1, teamID);
            rs = stmt.executeQuery();
            if (rs.next()) {
                teamStats.teamID = rs.getInt(1);
                teamStats.goals = rs.getInt(2);
                teamStats.shots = rs.getInt(3);
                teamStats.shotsOnGoal = rs.getInt(4);
                teamStats.goalsAgainst = rs.getInt(5);
                teamStats.fouls = rs.getInt(6);
                teamStats.yellow = rs.getInt(7);
                teamStats.red = rs.getInt(8);
            }
            success = true;
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (success) {
            ResponseBean resp = new ResponseBean(200, "");
            resp.setTeamStats(teamStats);
            return resp;
        }
        return new ResponseBean(500, "Unable to retrieve statistics.");
    }
}
