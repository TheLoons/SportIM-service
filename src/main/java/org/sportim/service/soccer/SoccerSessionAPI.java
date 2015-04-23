package org.sportim.service.soccer;

import org.sportim.service.api.StatSessionAPI;
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
 * API for starting a stat tracking session for a soccer event.
 *
 * This is a deprecated API. Use {@link org.sportim.service.api.StatSessionAPI} instead.
 */
@Path("/session")
public class SoccerSessionAPI {
    private StatSessionAPI statSessionAPI;

    public SoccerSessionAPI() {
        statSessionAPI = new StatSessionAPI();
    }

    public SoccerSessionAPI(StatSessionAPI statSessionAPI) {
        this.statSessionAPI = statSessionAPI;
    }

    @GET
    @Path("{eventID}")
    @Produces("application/json")
    @Deprecated
    public ResponseBean startEventSession(@PathParam("eventID") final int eventID,
                                          @HeaderParam("token") final String token) {
        return statSessionAPI.startEventSession(eventID, token);
    }

    @GET
    @Path("/reset/{eventID}")
    @Produces("application/json")
    @Deprecated
    public ResponseBean restartEventSession(@PathParam("eventID") final int eventID,
                                            @HeaderParam("token") final String token) {
        return statSessionAPI.restartEventSession(eventID, token);
    }

    @DELETE
    @Path("{eventID}")
    @Produces("application/json")
    @Deprecated
    public ResponseBean endEventSession(@PathParam("eventID") final int eventID,
                                        @HeaderParam("token") final String token,
                                        @HeaderParam("session") final String session) {
        return statSessionAPI.endEventSession(eventID, token, session);
    }
}
