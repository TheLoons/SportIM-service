package org.sportim.service.api;

import org.joda.time.DateTime;
import org.sportim.service.beans.EventBean;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.ConnectionManager;
import org.sportim.service.util.ConnectionProvider;

import javax.ws.rs.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * API to handle bulk event requests.
 * Created by hannah on 11/26/14.
 */
@Path("/events")
public class MultiEventAPI {
    private ConnectionProvider provider;

    public MultiEventAPI() {
        provider = ConnectionManager.getInstance();
    }

    public MultiEventAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean batchPostEvents(List<EventBean> events) {
        ResponseBean resp = new ResponseBean(200, "");
        List<Integer> ids = new ArrayList<Integer>(events.size());
        for (EventBean event : events) {
            ResponseBean sresp = SingleEventAPI.createDBEvent(event, provider);
            if (resp.getStatus().getCode() != 200) {
                return sresp;
            }
            ids.add(sresp.getId());
        }
        resp.setIds(ids);
        return resp;
    }

    @GET
    @Produces("application/json")
    public ResponseBean getEventsForRange(@QueryParam(value = "start") final String start,
                                          @QueryParam(value = "end") final String end,
                                          @QueryParam(value = "token") final String authToken) {
        int status = 200;
        String message = "";

        if (start == null || start.isEmpty() || end == null || end.isEmpty()) {
            status = 400;
            message = "You must specify a start and end date.";
            return new ResponseBean(status, message);
        }

        DateTime startTime;
        DateTime endTime;
        try {
            startTime = APIUtils.parseDateTime(start);
            endTime = APIUtils.parseDateTime(end);
        } catch (IllegalArgumentException e) {
            status = 400;
            message = "Invalid date format.";
            return new ResponseBean(status, message);
        }

        // TODO only return authorized events via auth token
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<EventBean> events = new LinkedList<EventBean>();
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT EventName, StartDate, EndDate, TournamentId, EventId FROM Event " +
                                         "WHERE StartDate < ? AND EndDate > ?");
            stmt.setLong(1, endTime.getMillis());
            stmt.setLong(2, startTime.getMillis());
            rs = stmt.executeQuery();

            while(rs.next()) {
                events.add(new EventBean(rs));
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve events. SQL error.";
            // TODO log4j 2 log this
            e.printStackTrace();
        } catch (NullPointerException e) {
            status = 500;
            message = "Unable to connect to datasource.";
            // TODO log4j 2 log this
            e.printStackTrace();
        } finally {
            boolean ok = APIUtils.closeResource(rs);
            ok = ok && APIUtils.closeResource(stmt);
            ok = ok && APIUtils.closeResource(conn);
            if (!ok) {
                // TODO implement Log4j 2 and log out error
            }
        }

        ResponseBean resp = new ResponseBean(status, message);
        resp.setEvents(events);
        return resp;
    }
}
