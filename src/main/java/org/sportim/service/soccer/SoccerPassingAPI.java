package org.sportim.service.soccer;

import org.sportim.service.api.PassingAPI;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.stats.PassBean;
import org.sportim.service.beans.stats.PlayerPassingBean;
import org.sportim.service.beans.stats.TeamPassingBean;
import org.sportim.service.util.*;

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
    private PassingAPI passingAPI;

    public SoccerPassingAPI() {
        provider = ConnectionManager.getInstance();
        passingAPI = new PassingAPI(provider);
    }

    public SoccerPassingAPI(ConnectionProvider provider) {
        this.provider = provider;
        passingAPI = new PassingAPI(provider);
    }

    @POST
    @Path("{eventID}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean postPass(final PassBean pass, @PathParam("eventID") final int eventID,
                                 @HeaderParam("token") final String token, @HeaderParam("session") final String session) {
        return passingAPI.postPass(pass, eventID, token, session);
    }

    @GET
    @Produces("application/json")
    public ResponseBean getPassingStats(@QueryParam("player") final String player, @QueryParam("teamID") final int teamID,
                                        @QueryParam("eventID") final int eventID, @HeaderParam("token") final String token) {
        return passingAPI.getPassingStats(player, teamID, eventID, token);
    }
}
