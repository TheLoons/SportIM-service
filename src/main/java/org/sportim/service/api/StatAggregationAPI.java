package org.sportim.service.api;

import org.apache.log4j.Logger;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.stats.*;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Gateway API to get at recorded statistics
 */
@Path("/stats")
public class StatAggregationAPI {
    private static Logger logger = Logger.getLogger(StatAggregationAPI.class.getName());
    private ConnectionProvider provider;
    private StatAPIMapper apiMapper;

    public StatAggregationAPI() {
        provider = ConnectionManager.getInstance();
        initialize();
    }

    public StatAggregationAPI(ConnectionProvider provider) {
        this.provider = provider;
        initialize();
    }

    private void initialize() {
        apiMapper = new StatAPIMapper(provider);
    }

    /**
     * Get the sport for an event
     * @param eventID the event ID
     * @return the SportType for the event
     */
    private SportType getEventSport(int eventID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT t.sport FROM Event e INNER JOIN TeamEvent te ON e.EventId = te.EventId " +
                    "INNER JOIN Team t ON te.TeamId = t.TeamId WHERE e.EventId = ?");
            stmt.setInt(1, eventID);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return SportType.fromString(rs.getString(1));
            }
            return SportType.UNKNOWN;
        } catch (Exception e) {
            logger.error("Error getting event sport: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            return SportType.UNKNOWN;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
    }

    /**
     * Get the sport for a team
     * @param teamID the team ID
     * @return the SportType for the team
     */
    private SportType getTeamSport(int teamID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT sport FROM Team WHERE TeamId = ?");
            stmt.setInt(1, teamID);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return SportType.fromString(rs.getString(1));
            }
            return SportType.UNKNOWN;
        } catch (Exception e) {
            logger.error("Error getting team sport: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            return SportType.UNKNOWN;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
    }

    /**
     * Get the sport for a league
     * @param leagueID the league ID
     * @return the SportType for the league
     */
    private SportType getLeagueSport(int leagueID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT sport FROM League WHERE LeagueId = ?");
            stmt.setInt(1, leagueID);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return SportType.fromString(rs.getString(1));
            }
            return SportType.UNKNOWN;
        } catch (Exception e) {
            logger.error("Error getting league sport: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            return SportType.UNKNOWN;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
    }

    private AggregationAPI getAPIForLeague(int teamID) {
        SportType sport = getLeagueSport(teamID);
        return apiMapper.getMainAPI(sport);
    }

    private AggregationAPI getAPIForTeam(int teamID) {
        SportType sport = getTeamSport(teamID);
        return apiMapper.getMainAPI(sport);
    }

    private AggregationAPI getAPIForEvent(int eventID) {
        SportType sport = getEventSport(eventID);
        return apiMapper.getMainAPI(sport);
    }

    private Set<SportType> getUserSports(String login) {
        Set<SportType> sports = new HashSet<SportType>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT Team.sport FROM Player INNER JOIN PlaysFor INNER JOIN Team " +
                    "WHERE Player.Login = ?");
            stmt.setString(1, login);
            rs = stmt.executeQuery();
            while (rs.next()) {
                sports.add(SportType.fromString(rs.getString(1)));
            }
        } catch (Exception e) {
            logger.error("Error getting user sports: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
        return sports;
    }

    private Set<AggregationAPI> getAPIsForUser(String login) {
        Set<AggregationAPI> apis = new HashSet<AggregationAPI>();
        Set<SportType> sports = getUserSports(login);
        for (SportType sport : sports) {
            AggregationAPI api = apiMapper.getMainAPI(sport);
            if (api != null) {
                apis.add(api);
            }
        }
        return apis;
    }

    /**
     * Delete an event's statistics
     * @param eventID path param, the event ID
     * @param token header param, the user's authentication token
     * @return ResponseBean with the status of the request
     */
    @DELETE
    @Produces("application/json")
    @Path("event/{eventID}")
    public ResponseBean deleteEventStats(@PathParam("eventID") final int eventID, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasEventTracking(token, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        AggregationAPI api = getAPIForEvent(eventID);
        if (api == null) {
            return new ResponseBean(400, "No statistics API found for event");
        }

        if (api.deleteEventStats(eventID)) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to delete event statistics");
    }

    /**
     * Get an event's statistics
     * @param eventID path param,the event ID
     * @param token header param, the user's authentication token
     * @return ResponseBean with the status of the request
     */
    @GET
    @Produces("application/json")
    @Path("event/{eventID}")
    public ResponseBean getEventStats(@PathParam("eventID") final int eventID, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasEventView(token, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        AggregationAPI api = getAPIForEvent(eventID);
        if (api == null) {
            return new ResponseBean(400, "No statistics API found for event");
        }

        AggregateEventBean stats = api.getEventStats(eventID);
        if (stats == null) {
            return new ResponseBean(500, "Unable to retrieve event statistics");
        }
        ResponseBean resp = new ResponseBean(200, "");
        resp.setEventStats(stats);
        return resp;
    }

    /**
     * Get a player's statistics
     * @param login query param, the player's login
     * @param teamID optional team ID to constrain results (get player's stats for only that team)
     * @param token header param, the user's authentication token
     * @return ResponseBean with the status of the request
     */
    @GET
    @Produces("application/json")
    @Path("player")
    public ResponseBean getPlayerStats(@QueryParam("login") final String login, @QueryParam("teamID") final int teamID,
                                       @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasUserView(token, login)) {
            return new ResponseBean(401, "Not authorized");
        }

        Set<AggregationAPI> apis = getAPIsForUser(login);
        if (apis == null || apis.isEmpty()) {
            return new ResponseBean(400, "User does not have statistics for any supported sports");
        }

        List<PlayerStatsBean> playerStats = new ArrayList<PlayerStatsBean>(apis.size());
        for (AggregationAPI api : apis) {
            PlayerStatsBean stats = api.getPlayerStats(login, teamID);
            if (stats != null) {
                playerStats.add(stats);
            }
        }
        ResponseBean resp = new ResponseBean(200, "");
        resp.setPlayerStats(playerStats);
        return resp;
    }

    /**
     * Get a team's statistics
     * @param teamID the team ID
     * @param token header param, the user's authentication token
     * @return ResponseBean with the status of the request
     */
    @GET
    @Produces("application/json")
    @Path("team/{teamID}")
    public ResponseBean getTeamStats(@PathParam("teamID") final int teamID, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasTeamView(token, teamID)) {
            return new ResponseBean(401, "Not authorized");
        }

        AggregationAPI api = getAPIForTeam(teamID);
        if (api == null) {
            return new ResponseBean(400, "No statistics API found for team");
        }

        TeamStatsBean stats = api.getTeamStats(teamID);
        if (stats == null) {
            return new ResponseBean(500, "Unable to retrieve event statistics");
        }
        ResponseBean resp = new ResponseBean(200, "");
        resp.setTeamStats(stats);
        return resp;
    }

    /**
     * Get a league's statistics
     * @param leagueID path param, the league ID
     * @param token header param, the user's authentication token
     * @return ResponseBean with the status of the request
     */
    @GET
    @Produces("application/json")
    @Path("league/{leagueID}")
    public ResponseBean getLeagueStats(@PathParam("leagueID") final int leagueID, @HeaderParam("token") final String token) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        AggregationAPI api = getAPIForLeague(leagueID);
        if (api == null) {
            return new ResponseBean(400, "No statistics API found for team");
        }

        LeagueStatsBean stats = api.getLeagueStats(leagueID);
        if (stats == null) {
            return new ResponseBean(500, "Unable to retrieve event statistics");
        }
        ResponseBean resp = new ResponseBean(200, "");
        resp.setLeagueStats(stats);
        return resp;
    }

}
