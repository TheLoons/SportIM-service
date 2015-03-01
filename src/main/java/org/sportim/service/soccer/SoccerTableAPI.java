package org.sportim.service.soccer;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.soccer.beans.TeamResultsBean;
import org.sportim.service.util.ConnectionManager;
import org.sportim.service.util.ConnectionProvider;
import org.sportim.service.util.PrivilegeUtil;

import javax.ws.rs.*;
import java.util.LinkedList;
import java.util.List;

/**
 * API for grabbing table results
 */
@Path("table")
public class SoccerTableAPI {
    private ConnectionProvider provider;

    public SoccerTableAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SoccerTableAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @GET
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean getTableForEvents(List<Integer> events, @HeaderParam("token") final String token) {
        for (int i : events) {
            if (!PrivilegeUtil.hasEventView(token, i)) {
                return new ResponseBean(401, "Not authorized");
            }
        }

        return new ResponseBean(501, "Not implemented");
    }

    public List<TeamResultsBean> getTableForEvents(List<Integer> events) {
        List<TeamResultsBean> table = new LinkedList<TeamResultsBean>();

        return table;
    }
}
