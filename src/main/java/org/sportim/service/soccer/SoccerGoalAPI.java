package org.sportim.service.soccer;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.soccer.beans.ScoreBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.AuthenticationUtil;
import org.sportim.service.util.ConnectionManager;
import org.sportim.service.util.ConnectionProvider;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * API for goal tracking
 */
@Path("/goal")
public class SoccerGoalAPI {
    private ConnectionProvider provider;

    public SoccerGoalAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SoccerGoalAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @POST
    @Path("{eventID}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean postGoal(final ScoreBean score, @PathParam("{eventID}") final int eventID,
                                 @HeaderParam("token") final String token, @HeaderParam("session") final String session) {
        if (AuthenticationUtil.validateToken(token) == null || !SoccerUtil.isValidSession(session, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT INTO SoccerStats (eventID, player, goals, shots, shotsongoal) VALUES (?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE goals = goals + 1, shots = shots + 1, shotsongoal = shotsongoal + 1");
            stmt.setInt(1, eventID);
            stmt.setString(2, score.scorer);
            stmt.setInt(3, 1);
            stmt.setInt(4, 1);
            stmt.setInt(5, 1);
            success = stmt.executeUpdate() > 0;

            APIUtils.closeResource(stmt);
            stmt = conn.prepareStatement("INSERT INTO SoccerStats (eventID, player, assists) VALUES (?,?,?) " +
                    "ON DUPLICATE KEY UPDATE assists = assists + 1");
            stmt.setInt(1, eventID);
            stmt.setString(2, score.assist);
            stmt.setInt(3, 1);
            success = success && (stmt.executeUpdate() > 0);

            APIUtils.closeResource(stmt);
            stmt = conn.prepareStatement("INSERT INTO SoccerStats (eventID, player, goalsagainst) VALUES (?,?,?) " +
                    "ON DUPLICATE KEY UPDATE goalsagainst = goalsagainst + 1");
            stmt.setInt(1, eventID);
            stmt.setString(2, score.goalkeeper);
            stmt.setInt(3, 1);
            success = success && (stmt.executeUpdate() > 0);
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (success) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to add score");
    }
}
