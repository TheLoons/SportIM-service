package org.sportim.service.soccer;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.soccer.beans.PassBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.AuthenticationUtil;
import org.sportim.service.util.ConnectionManager;
import org.sportim.service.util.ConnectionProvider;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

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
}
