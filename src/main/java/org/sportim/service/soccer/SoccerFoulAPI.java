package org.sportim.service.soccer;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.soccer.beans.FoulBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.AuthenticationUtil;
import org.sportim.service.util.ConnectionManager;
import org.sportim.service.util.ConnectionProvider;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * API for tracking fouls/cards for soccer events
 */
@Path("/foul")
public class SoccerFoulAPI {
    private static final String UPDATE_QUERY_BASE = "INSERT INTO SoccerStats (eventID, player, fouls%s) VALUES " +
                                                    "(?, ?, ?%s) ON DUPLICATE KEY UPDATE fouls = fouls + 1%s";
    private ConnectionProvider provider;

    public SoccerFoulAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SoccerFoulAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @POST
    @Path("{eventID}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean postFoul(final FoulBean foul, @PathParam("eventID") final int eventID,
                                 @HeaderParam("token") final String token, @HeaderParam("session") final String session) {
        if (AuthenticationUtil.validateToken(token) == null || !SoccerUtil.isValidSession(session, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!foul.validate()) {
            return new ResponseBean(400, "Malformed request");
        }

        boolean success = true;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = createUpdateQuery(eventID, foul, conn);
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
        return new ResponseBean(500, "Unable to add foul");
    }

    /**
     * Create an update query for the given Foul
     * @param foul the FoulBean we're using to update
     * @param conn the database connection
     * @return a PreparedStatement for updating the database based on the foul
     * @throws SQLException
     */
    public PreparedStatement createUpdateQuery(final int eventID, final FoulBean foul,
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
        stmt.setString(2, foul.player);
        stmt.setInt(3, 1);
        if (foul.red || foul.yellow) {
            stmt.setInt(4, 1);
        }
        if (foul.red && foul.yellow) {
            stmt.setInt(5, 1);
        }
        return stmt;
    }
}
