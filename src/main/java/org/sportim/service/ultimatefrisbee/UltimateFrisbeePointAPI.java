package org.sportim.service.ultimatefrisbee;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.ultimatefrisbee.beans.UltimateScoreBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

@Path("/point")
public class UltimateFrisbeePointAPI {
    private ConnectionProvider provider;

    public UltimateFrisbeePointAPI() {
        provider = ConnectionManager.getInstance();
    }

    public UltimateFrisbeePointAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @POST
    @Path("{eventID}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean postGoal(final UltimateScoreBean score, @PathParam("eventID") final int eventID,
                                 @HeaderParam("token") final String token, @HeaderParam("session") final String session) {
        if (AuthenticationUtil.validateToken(token) == null || !StatUtil.isValidSession(session, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!score.validate()) {
            return new ResponseBean(400, "Malformed request");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT INTO UltimateStats (eventID, teamID, player, pointsthrown) VALUES (?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE pointsthrown = pointsthrown + 1");
            stmt.setInt(1, eventID);
            stmt.setInt(2, score.teamID);
            stmt.setString(3, score.thrower);
            stmt.setInt(4, 1);
            success = stmt.executeUpdate() > 0;

            APIUtils.closeResource(stmt);
            stmt = conn.prepareStatement("INSERT INTO UltimateStats (eventID, teamID, player, pointsreceived) VALUES (?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE pointsreceived = pointsreceived + 1");
            stmt.setInt(1, eventID);
            stmt.setInt(2, score.teamID);
            stmt.setString(3, score.receiver);
            stmt.setInt(4, 1);
            success = success && (stmt.executeUpdate() > 0);

            APIUtils.closeResource(stmt);
            stmt = conn.prepareStatement("INSERT INTO UltimateTeamStats (eventID, teamID, pointsagainst) VALUES (?,?,?) " +
                    "ON DUPLICATE KEY UPDATE pointsagainst = pointsagainst + 1");
            stmt.setInt(1, eventID);
            stmt.setInt(2, score.opposingTeamID);
            stmt.setInt(3, 1);
            success = success && (stmt.executeUpdate() > 0);
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
            success = false;
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (success) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to add score");
    }
}
