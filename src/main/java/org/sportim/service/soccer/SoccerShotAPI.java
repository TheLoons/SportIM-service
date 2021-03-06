package org.sportim.service.soccer;

import org.apache.log4j.Logger;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.soccer.beans.SoccerShotBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * API for shot tracking
 */
@Path("/shot")
public class SoccerShotAPI {
    private static Logger logger = Logger.getLogger(SoccerShotAPI.class.getName());
    private ConnectionProvider provider;

    public SoccerShotAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SoccerShotAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    /**
     * Record a shot
     * @param shot body param, contains shot info
     * @param eventID path param, the event ID
     * @param token header param, the user's authentication token
     * @param session header param, the user's stat tracking session ID
     * @return a ResponseBean containing the result status
     */
    @POST
    @Path("{eventID}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean postShot(final SoccerShotBean shot, @PathParam("eventID") final int eventID,
                                 @HeaderParam("token") final String token, @HeaderParam("session") final String session) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!StatUtil.isValidSession(session, eventID)) {
            return new ResponseBean(400, "You must start a session before tracking any statistics");
        }

        if (!shot.validate()) {
            return new ResponseBean(400, "Malformed request");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            conn = provider.getConnection();
            if (shot.onGoal) {
                stmt = conn.prepareStatement("INSERT INTO SoccerStats (eventID, teamID, player, shots, shotsongoal) VALUES (?,?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE shots = shots + 1, shotsongoal = shotsongoal + 1");
                stmt.setInt(1, eventID);
                stmt.setInt(2, shot.teamID);
                stmt.setString(3, shot.player);
                stmt.setInt(4, 1);
                stmt.setInt(5, 1);
            } else {
                stmt = conn.prepareStatement("INSERT INTO SoccerStats (eventID, teamID, player, shots) VALUES (?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE shots = shots + 1");
                stmt.setInt(1, eventID);
                stmt.setInt(2, shot.teamID);
                stmt.setString(3, shot.player);
                stmt.setInt(4, 1);
            }
            success = stmt.executeUpdate() > 0;

            APIUtils.closeResource(stmt);
            if (shot.onGoal && success) {
                stmt = conn.prepareStatement("INSERT INTO SoccerStats (eventID, teamID, player, saves) VALUES (?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE saves = saves + 1");
                stmt.setInt(1, eventID);
                stmt.setInt(2, shot.goalieTeamID);
                stmt.setString(3, shot.goalkeeper);
                stmt.setInt(4, 1);
                success = stmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            logger.error("Unable to post shot: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            success = false;
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (success) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to add shot");
    }
}
