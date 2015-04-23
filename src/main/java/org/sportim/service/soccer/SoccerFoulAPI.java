package org.sportim.service.soccer;

import org.apache.log4j.Logger;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.soccer.beans.SoccerFoulBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * API for tracking fouls/cards for soccer events
 */
@Path("/foul")
public class SoccerFoulAPI {
    private static Logger logger = Logger.getLogger(SoccerFoulAPI.class.getName());
    private static final String UPDATE_QUERY_BASE = "INSERT INTO SoccerStats (eventID, teamID, player, fouls%s) VALUES " +
                                                    "(?, ?, ?, ?%s) ON DUPLICATE KEY UPDATE fouls = fouls + 1%s";
    private ConnectionProvider provider;

    public SoccerFoulAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SoccerFoulAPI(ConnectionProvider provider) {
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
    public ResponseBean postFoul(final SoccerFoulBean foul, @PathParam("eventID") final int eventID,
                                 @HeaderParam("token") final String token, @HeaderParam("session") final String session) {
        if (AuthenticationUtil.validateToken(token) == null || !StatUtil.isValidSession(session, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!foul.validate()) {
            return new ResponseBean(400, "Malformed request");
        }

        boolean success = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = createUpdateQuery(eventID, foul, conn);
            success = stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Unable to post soccer foul: " + e.getMessage());
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

    /**
     * Create an update query for the given Foul
     * @param foul the FoulBean we're using to update
     * @param conn the database connection
     * @return a PreparedStatement for updating the database based on the foul
     * @throws SQLException
     */
    public PreparedStatement createUpdateQuery(final int eventID, final SoccerFoulBean foul,
                                               Connection conn) throws SQLException {
        String extraColNames = "";
        String params = "";
        String onUpdate = "";
        if (foul.red) {
            extraColNames += ", red";
            params += ", ?";
            onUpdate += ", red = red + 1";
        }
        if (foul.yellow) {
            extraColNames += ", yellow";
            params += ", ?";
            onUpdate += ", yellow = yellow + 1";
        }

        String query = String.format(UPDATE_QUERY_BASE, extraColNames, params, onUpdate);
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, eventID);
        stmt.setInt(2, foul.teamID);
        stmt.setString(3, foul.player);
        stmt.setInt(4, 1);
        if (foul.red || foul.yellow) {
            stmt.setInt(5, 1);
        }
        if (foul.red && foul.yellow) {
            stmt.setInt(6, 1);
        }
        return stmt;
    }
}
