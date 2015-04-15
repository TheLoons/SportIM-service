package org.sportim.service.ultimatefrisbee;

import org.apache.log4j.Logger;
import org.sportim.service.api.AggregationAPI;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.stats.AggregateEventBean;
import org.sportim.service.beans.stats.LeagueStatsBean;
import org.sportim.service.beans.stats.PlayerStatsBean;
import org.sportim.service.beans.stats.TeamStatsBean;
import org.sportim.service.ultimatefrisbee.beans.UltimateEventBean;
import org.sportim.service.ultimatefrisbee.beans.UltimateLeagueStatsBean;
import org.sportim.service.ultimatefrisbee.beans.UltimatePlayerStatsBean;
import org.sportim.service.ultimatefrisbee.beans.UltimateTeamStatsBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Path("/stats")
public class UltimateFrisbeeAggregationAPI implements AggregationAPI {
    private static Logger logger = Logger.getLogger(UltimateFrisbeeAggregationAPI.class.getName());
    private ConnectionProvider provider;

    public UltimateFrisbeeAggregationAPI() {
        provider = ConnectionManager.getInstance();
    }

    public UltimateFrisbeeAggregationAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @DELETE
    @Produces("application/json")
    @Path("event/{eventID}")
    public ResponseBean deleteEventStatsRest(@PathParam("eventID") final int eventID, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasEventTracking(token, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        if (deleteEventStats(eventID)) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to delete event statistics");
    }

    @Override
    public boolean deleteEventStats(int eventID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("DELETE FROM UltimateStats WHERE eventID = ?");
            stmt.setInt(1, eventID);
            stmt.executeUpdate();
            APIUtils.closeResources(stmt);
            stmt = conn.prepareStatement("DELETE FROM Passing WHERE eventID = ?");
            stmt.setInt(1, eventID);
            stmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Unable to delete ultimate stats: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            return false;
        } finally {
            APIUtils.closeResources(stmt, conn);
        }
        return true;
    }

    @GET
    @Produces("application/json")
    @Path("event/{eventID}")
    public ResponseBean getEventStatsRest(@PathParam("eventID") final int eventID, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasEventView(token, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        AggregateEventBean eventStats = getEventStats(eventID);
        if (eventStats != null) {
            ResponseBean resp = new ResponseBean(200, "");
            resp.setEventStats(eventStats);
            return resp;
        }
        return new ResponseBean(500, "Unable to retrieve statistics.");
    }

    @Override
    public AggregateEventBean getEventStats(int eventID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        UltimateEventBean eventStats = new UltimateEventBean(eventID);
        boolean success = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT teamID, SUM(pointsthrown), SUM(fouls) FROM UltimateStats " +
                    "WHERE eventID = ? " +
                    "GROUP BY teamID");
            stmt.setInt(1, eventID);
            rs = stmt.executeQuery();
            eventStats.teamStats = new ArrayList<TeamStatsBean>(2);
            while(rs.next()) {
                UltimateTeamStatsBean teamStats = new UltimateTeamStatsBean(rs.getInt(1));
                teamStats.pointsFor = rs.getInt(2);
                teamStats.fouls = rs.getInt(3);

                // Add points against for any other team
                for (TeamStatsBean other : eventStats.teamStats) {
                    ((UltimateTeamStatsBean)other).pointsAgainst += teamStats.pointsFor;
                }

                eventStats.teamStats.add(teamStats);
                eventStats.totalPoints += teamStats.pointsFor;
            }

            APIUtils.closeResources(rs, stmt);
            for (TeamStatsBean team : eventStats.teamStats) {
                stmt = conn.prepareStatement("SELECT player, SUM(pointsthrown), SUM(pointsreceived), SUM(fouls) FROM UltimateStats " +
                        "WHERE eventID = ? AND teamID = ? " +
                        "GROUP BY player");
                stmt.setInt(1, eventID);
                stmt.setInt(2, team.teamID);
                rs = stmt.executeQuery();
                team.playerStats = new ArrayList<PlayerStatsBean>();
                while(rs.next()) {
                    UltimatePlayerStatsBean playerStats = new UltimatePlayerStatsBean(rs.getString(1));
                    playerStats.pointsThrown = rs.getInt(2);
                    playerStats.pointsReceived = rs.getInt(3);
                    playerStats.fouls = rs.getInt(4);
                    team.playerStats.add(playerStats);
                }
                APIUtils.closeResources(rs, stmt);
            }
            success = true;
        } catch (Exception e) {
            logger.error("Unable to get ultimate event stats: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (success) {
            return eventStats;
        }
        return null;
    }

    @GET
    @Produces("application/json")
    @Path("player")
    public ResponseBean getPlayerStatsRest(@QueryParam("login") final String login, @QueryParam("teamID") final int teamID,
                                           @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasUserView(token, login)) {
            return new ResponseBean(401, "Not authorized");
        }

        PlayerStatsBean playerStats = getPlayerStats(login, teamID);

        if (playerStats != null) {
            ResponseBean resp = new ResponseBean(200, "");
            List<PlayerStatsBean> stats = new ArrayList<PlayerStatsBean>(1);
            stats.add(playerStats);
            resp.setPlayerStats(stats);
            return resp;
        }
        return new ResponseBean(500, "Unable to retrieve statistics.");
    }

    @Override
    public PlayerStatsBean getPlayerStats(String login, int teamID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        UltimatePlayerStatsBean playerStats = null;
        try {
            conn = provider.getConnection();
            if (teamID < 1) {
                stmt = conn.prepareStatement("SELECT player, SUM(pointsthrown), SUM(pointsreceived), SUM(fouls) FROM UltimateStats " +
                        "WHERE player = ?" +
                        "GROUP BY player");
                stmt.setString(1, login);
            } else {
                stmt = conn.prepareStatement("SELECT player, SUM(pointsthrown), SUM(pointsreceived), SUM(fouls) FROM UltimateStats " +
                        "WHERE player = ? AND teamID = ? " +
                        "GROUP BY player");
                stmt.setString(1, login);
                stmt.setInt(2, teamID);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                playerStats = new UltimatePlayerStatsBean(login);
                playerStats.pointsThrown = rs.getInt(2);
                playerStats.pointsReceived = rs.getInt(3);
                playerStats.fouls = rs.getInt(4);
            }
        } catch (Exception e) {
            logger.error("Unable to get ultimate player stats: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            playerStats = null;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
        return playerStats;
    }

    @GET
    @Produces("application/json")
    @Path("team/{teamID}")
    public ResponseBean getTeamsStatsRest(@PathParam("teamID") final int teamID, @HeaderParam("token") final String token) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        TeamStatsBean teamStats = getTeamStats(teamID);
        if (teamStats != null) {
            ResponseBean resp = new ResponseBean(200, "");
            resp.setTeamStats(teamStats);
            return resp;
        }
        return new ResponseBean(500, "Unable to retrieve statistics.");
    }

    @Override
    public TeamStatsBean getTeamStats(int teamID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        UltimateTeamStatsBean teamStats = new UltimateTeamStatsBean(teamID);
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT teamID, SUM(pointsthrown), SUM(fouls) FROM UltimateStats " +
                    "WHERE teamID = ? " +
                    "GROUP BY teamID");
            stmt.setInt(1, teamID);
            rs = stmt.executeQuery();
            if (rs.next()) {
                teamStats.pointsFor = rs.getInt(2);
                teamStats.fouls = rs.getInt(3);
            }
            APIUtils.closeResources(rs, stmt);

            stmt = conn.prepareStatement("SELECT eventID, SUM(pointsthrown) FROM UltimateStats " +
                    "WHERE teamID != ? " +
                    "GROUP BY eventID");
            stmt.setInt(1, teamID);
            rs = stmt.executeQuery();
            if (rs.next()) {
                teamStats.pointsAgainst = rs.getInt(2);
            }
        } catch (Exception e) {
            logger.error("Unable to get ultimate team stats: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            return null;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        return teamStats;
    }

    @GET
    @Produces("application/json")
    @Path("league/{leagueID}")
    public ResponseBean getLeagueStatsRest(@PathParam("leagueID") final int leagueID, @HeaderParam("token") final String token) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        LeagueStatsBean leagueStats = getLeagueStats(leagueID);
        if (leagueStats == null) {
            return new ResponseBean(500, "Unable to retrieve league stats.");
        }

        ResponseBean resp = new ResponseBean(200, "");
        resp.setLeagueStats(leagueStats);
        return resp;
    }

    @Override
    public LeagueStatsBean getLeagueStats(int leagueID) {
        List<Integer> teams = StatUtil.getAllTeamsInLeague(leagueID);
        if (teams == null) {
            return null;
        }

        UltimateLeagueStatsBean leagueStats = new UltimateLeagueStatsBean(leagueID);
        leagueStats.teamStats = new ArrayList<TeamStatsBean>(teams.size());
        for (Integer teamID : teams) {
            TeamStatsBean teamStatsGen = getTeamStats(teamID);
            if (teamStatsGen != null) {
                UltimateTeamStatsBean teamStats = (UltimateTeamStatsBean)teamStatsGen;
                leagueStats.teamStats.add(teamStats);
                if (teamStats.pointsFor > leagueStats.topTeamScore) {
                    leagueStats.topScoringTeam = teamID;
                    leagueStats.topTeamScore = teamStats.pointsFor;
                }
                leagueStats.points += teamStats.pointsFor;
                leagueStats.fouls += teamStats.fouls;
            }
        }
        return leagueStats;
    }

    @Override
    public int getEventWinner(int eventID, Set<Integer> losers) {
        losers.clear();

        AggregateEventBean event = getEventStats(eventID);
        if (event == null || !(event instanceof UltimateEventBean)) {
            return -1;
        }

        UltimateEventBean eventStats = (UltimateEventBean)event;
        int winner = -1;
        int maxScore = -1;
        losers.clear();
        for (TeamStatsBean teamGen : eventStats.teamStats) {
            UltimateTeamStatsBean team = (UltimateTeamStatsBean)teamGen;
            if (team.pointsFor > maxScore) {
                if (winner != -1) {
                    losers.add(winner);
                }
                winner = team.teamID;
                maxScore = team.pointsFor;
            } else {
                losers.add(team.teamID);
            }
        }
        return winner;
    }
}
