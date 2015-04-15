package org.sportim.service.api;

import org.apache.log4j.Logger;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.ConnectionManager;
import org.sportim.service.util.ConnectionProvider;
import org.sportim.service.util.PrivilegeUtil;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/**
 * API for starting a stat tracking session for an event.
 */
@Path("/session")
public class StatSessionAPI {
    private static Logger logger = Logger.getLogger(StatSessionAPI.class.getName());
    private ConnectionProvider provider;

    public StatSessionAPI() {
        provider = ConnectionManager.getInstance();
    }

    public StatSessionAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @GET
    @Path("{eventID}")
    @Produces("application/json")
    public ResponseBean startEventSession(@PathParam("eventID") final int eventID,
                                          @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasEventTracking(token, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        String sessionID = UUID.randomUUID().toString();

        Connection conn = null;
        PreparedStatement stmt = null;
        int res = -1;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT IGNORE INTO StatSessions (eventID, sessionID) VALUES (?,?)");
            stmt.setInt(1, eventID);
            stmt.setString(2, sessionID);
            res = stmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Error starting stat session: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        ResponseBean resp;
        if (res == 1) {
            resp = new ResponseBean(200, "");
            resp.setSession(sessionID);
        } else if (res == 0) {
            resp = new ResponseBean(409, "Session already started");
        } else {
            resp = new ResponseBean(500, "Unable to start session");
        }
        return resp;
    }

    @GET
    @Path("/reset/{eventID}")
    @Produces("application/json")
    public ResponseBean restartEventSession(@PathParam("eventID") final int eventID,
                                            @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasEventTracking(token, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        String sessionID = UUID.randomUUID().toString();

        Connection conn = null;
        PreparedStatement stmt = null;
        int res = -1;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT INTO StatSessions (eventID, sessionID) VALUES (?,?) " +
                    "ON DUPLICATE KEY UPDATE sessionID = ?");
            stmt.setInt(1, eventID);
            stmt.setString(2, sessionID);
            stmt.setString(3, sessionID);
            res = stmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Error restarting stat session: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (res < 1) {
            return new ResponseBean(500, "Unable to reset stat session");
        }
        ResponseBean resp = new ResponseBean(200, "");
        resp.setSession(sessionID);
        return resp;
    }

    @DELETE
    @Path("{eventID}")
    @Produces("application/json")
    public ResponseBean endEventSession(@PathParam("eventID") final int eventID,
                                        @HeaderParam("token") final String token,
                                        @HeaderParam("session") final String session) {
        if (!PrivilegeUtil.hasEventTracking(token, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        int res = -1;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("DELETE FROM StatSessions WHERE eventID = ? AND sessionID = ?");
            stmt.setInt(1, eventID);
            stmt.setString(2, session);
            res = stmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Error ending stat session: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (res < 1) {
            return new ResponseBean(500, "Unable to end stat session");
        }
        return new ResponseBean(200, "");
    }
}