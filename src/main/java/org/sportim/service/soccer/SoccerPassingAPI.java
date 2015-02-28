package org.sportim.service.soccer;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.soccer.beans.PassBean;
import org.sportim.service.soccer.beans.PlayerPassingBean;
import org.sportim.service.soccer.beans.TeamPassingBean;
import org.sportim.service.util.*;
import sun.net.www.protocol.http.AuthenticationInfo;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * API for tracking passes in soccer events.
 */
@Path("/pass")
public class SoccerPassingAPI {
    private ConnectionProvider provider;

    public SoccerPassingAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SoccerPassingAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @POST
    @Path("{eventID}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean postPass(final PassBean pass, @PathParam("eventID") final int eventID,
                                 @HeaderParam("token") final String token, @HeaderParam("session") final String session) {
        if (AuthenticationUtil.validateToken(token) == null || !SoccerUtil.isValidSession(session, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!pass.validate()) {
            return new ResponseBean(400, "Malformed request");
        }

        boolean success = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT INTO SoccerPassing (`to`, `from`, eventID, passes) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE passes = passes + 1");
            stmt.setString(1, pass.to);
            stmt.setString(2, pass.from);
            stmt.setInt(3, eventID);
            stmt.setInt(4, 1);
            success = stmt.executeUpdate() > 0;
        } catch (Exception e) {
            // TODO log
             e.printStackTrace();
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (success) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to add pass");
    }

    @GET
    @Produces("application/json")
    public ResponseBean getPassingStats(@QueryParam("player") final String player, @QueryParam("teamID") final int teamID,
                                        @QueryParam("eventID") final int eventID, @HeaderParam("token") final String token) {
        if (player != null) {
            if (!PrivilegeUtil.hasUserView(token, player)) {
                return new ResponseBean(401, "Not authorized");
            } else {
                PlayerPassingBean passes = getPlayerPassingStats(player);
                if (passes != null) {
                    ResponseBean resp = new ResponseBean(200, "");
                    resp.setPlayerPasses(passes);
                    return resp;
                }
                return new ResponseBean(500, "Unable to retrieve passing statistics.");
            }
        }

        if (eventID > 0) {
            if (!PrivilegeUtil.hasEventView(token, eventID)) {
                return new ResponseBean(401, "Not authorized");
            } else {
                List<TeamPassingBean> passes = getEventPassingStats(eventID);
                if (passes != null) {
                    ResponseBean resp = new ResponseBean(200, "");
                    resp.setEventPasses(passes);
                    return resp;
                }
                return new ResponseBean(500, "Unable to retrieve passing statistics.");
            }
        }

        if (teamID > 0) {
            if (!PrivilegeUtil.hasTeamView(token, teamID)) {
                return new ResponseBean(401, "Not authorized");
            } else {
                TeamPassingBean passes = getTeamPassingStats(teamID);
                if (passes != null) {
                    ResponseBean resp = new ResponseBean(200, "");
                    resp.setTeamPasses(passes);
                    return resp;
                }
                return new ResponseBean(500, "Unable to retrieve passing statistics.");
            }
        }

        return new ResponseBean(400, "You must specify a player, team or event");
    }

    private List<TeamPassingBean> getEventPassingStats(final int eventID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<TeamPassingBean> eventPasses = new ArrayList<TeamPassingBean>();
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT DISTINCT sp.to, sp.from, ss.teamID, sp.passes " +
                    "FROM SoccerPassing sp INNER JOIN SoccerStats ss " +
                    "ON sp.eventID = ss.eventID AND (sp.from = ss.player OR sp.to = ss.player) " +
                    "WHERE sp.eventID = ? ");
            stmt.setInt(1, eventID);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int teamID = rs.getInt(3);
                int count = rs.getInt(4);
                TeamPassingBean teamPasses = new TeamPassingBean(teamID);
                int i = eventPasses.indexOf(teamPasses);
                if (i >= 0) {
                    teamPasses = eventPasses.get(i);
                } else {
                    eventPasses.add(teamPasses);
                }
                teamPasses.totalPasses += count;
                PassBean pass = new PassBean();
                pass.to = rs.getString(1);
                pass.from = rs.getString(2);
                pass.count = count;
                teamPasses.passes.add(pass);
            }
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
            eventPasses = null;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
        return eventPasses;
    }

    private PlayerPassingBean getPlayerPassingStats(final String player) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        PlayerPassingBean passes = new PlayerPassingBean();
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT SoccerPassing.to, SoccerPassing.from, passes " +
                    "FROM SoccerPassing " +
                    "WHERE SoccerPassing.to = ? OR SoccerPassing.from = ?");
            stmt.setString(1, player);
            stmt.setString(2, player);
            rs = stmt.executeQuery();

            passes.player = player;
            passes.passesMade = new HashMap<String, Integer>();
            passes.passesReceived = new HashMap<String, Integer>();
            while (rs.next()) {
                String to = rs.getString(1);
                String from = rs.getString(2);
                int passCount = rs.getInt(3);
                if (player.equals(to)) {
                    Integer count = passes.passesReceived.get(from);
                    passes.passesReceived.put(from, incrementCount(count, passCount));
                    passes.totalPassesReceived += passCount;
                } else {
                    Integer count = passes.passesMade.get(to);
                    passes.passesMade.put(to, incrementCount(count, passCount));
                    passes.totalPassesMade += passCount;
                }
            }
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
            passes = null;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
        return passes;
    }

    private TeamPassingBean getTeamPassingStats(final int teamID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        TeamPassingBean passes = new TeamPassingBean();
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT sp.to, sp.from, SUM(sp.passes) " +
                    "FROM SoccerPassing sp INNER JOIN SoccerStats ss " +
                    "ON sp.eventID = ss.eventID AND sp.to = ss.player " +
                    "WHERE ss.teamID = ? " +
                    "GROUP BY sp.to, sp.from");
            stmt.setInt(1, teamID);
            rs = stmt.executeQuery();

            passes.teamID = teamID;
            passes.passes =  new ArrayList<PassBean>();
            while (rs.next()) {
                PassBean pass = new PassBean();
                pass.to = rs.getString(1);
                pass.from = rs.getString(2);
                pass.count = rs.getInt(3);
                passes.passes.add(pass);
            }
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
            passes = null;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
        return passes;
    }

    private int incrementCount(Integer count, int passCount) {
        if (count == null) {
            return passCount;
        }
        return count + passCount;
    }
}
