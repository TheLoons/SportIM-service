package org.sportim.service.ultimatefrisbee;

import org.apache.log4j.Logger;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.stats.FoulBean;
import org.sportim.service.soccer.beans.SoccerFoulBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * API for tracking ultimate frisbee fouls
 */
@Path("/foul")
public class UltimateFrisbeeFoulAPI {
    private static Logger logger = Logger.getLogger(UltimateFrisbeeFoulAPI.class.getName());
    private ConnectionProvider provider;

    public UltimateFrisbeeFoulAPI() {
        provider = ConnectionManager.getInstance();
    }

    public UltimateFrisbeeFoulAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    /**
     * Record a foul
     * @param foul body param, contains foul info
     * @param eventID path param, the event ID
     * @param token header param, the user's authentication token
     * @param session header param, the user's stat tracking session ID
     * @return a ResponseBean containing the result status
     */
    @POST
    @Path("{eventID}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean postFoul(final FoulBean foul, @PathParam("eventID") final int eventID,
                                 @HeaderParam("token") final String token, @HeaderParam("session") final String session) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!StatUtil.isValidSession(session, eventID)) {
            return new ResponseBean(400, "You must start a session before tracking any statistics");
        }

        if (!foul.validate()) {
            return new ResponseBean(400, "Malformed request");
        }

        boolean success = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT INTO UltimateStats (eventID, teamID, player, fouls) VALUES (?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE fouls = fouls + 1");
            stmt.setInt(1, eventID);
            stmt.setInt(2, foul.teamID);
            stmt.setString(3, foul.player);
            stmt.setInt(4, 1);
            success = stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Unable to post ultimate foul: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            success = false;
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (success) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to add foul");
    }
}
