package org.sportim.service.ultimatefrisbee;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.util.AuthenticationUtil;
import org.sportim.service.util.ConnectionManager;
import org.sportim.service.util.ConnectionProvider;
import org.sportim.service.util.StatUtil;

import javax.ws.rs.*;
import java.util.HashSet;
import java.util.Set;

/**
 * API used to finalize ultimate frisbee event results
 */
@Path("finalize")
public class UltimateFrisbeeFinalizeAPI {
    private ConnectionProvider provider;
    private UltimateFrisbeeAggregationAPI ultimateStatAPI;

    public UltimateFrisbeeFinalizeAPI() {
        provider = ConnectionManager.getInstance();
        ultimateStatAPI = new UltimateFrisbeeAggregationAPI(provider);
    }

    public UltimateFrisbeeFinalizeAPI(ConnectionProvider provider) {
        this.provider = provider;
        ultimateStatAPI = new UltimateFrisbeeAggregationAPI(provider);
    }

    /**
     * Finalize the event (triggers bracket calculations)
     * @param eventID path param, the event ID
     * @param token header param, the user's authentication token
     * @param session header param, the user's stats tracking session ID
     * @return a ResponseBean with the result status
     */
    @POST
    @Path("{eventID}")
    @Produces("application/json")
    public ResponseBean finalize(@PathParam("eventID") final int eventID, @HeaderParam("token") final String token,
                                 @HeaderParam("session") final String session) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        if (!StatUtil.isValidSession(session, eventID)) {
            return new ResponseBean(400, "You must start a session before tracking any statistics");
        }

        Set<Integer> losers = new HashSet<Integer>();
        int winner = ultimateStatAPI.getEventWinner(eventID, losers);
        if (winner != -1) {
            StatUtil.fillNextBracketEvent(eventID, winner, losers);
        }
        return new ResponseBean(200, "");
    }
}
