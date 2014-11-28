package org.sportim.service;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.ConnectionManager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.sql.*;

/**
 * API to handle bulk event requests.
 * Created by hannah on 11/26/14.
 */
@Path("/events")
public class MultiEventAPI {

    @GET
    @Produces("application/json")
    public String getEventsForRange(@QueryParam(value = "start") final String start,
                                    @QueryParam(value = "end") final String end,
                                    @QueryParam(value = "token") final String authToken) {
        int status = 200;
        String message = "";
        JSONObject response = new JSONObject();
        JSONArray events = new JSONArray();
        response.put("events", events);

        if (start == null || start.isEmpty() || end == null || end.isEmpty()) {
            status = 400;
            message = "You must specify a start and end date.";
            APIUtils.appendStatus(response, status, message);
            return response.toString();
        }

        DateTime startTime;
        DateTime endTime;
        try {
            startTime = APIUtils.parseDateTime(start);
            endTime = APIUtils.parseDateTime(end);
        } catch (IllegalArgumentException e) {
            status = 400;
            message = "Invalid date format.";
            APIUtils.appendStatus(response, status, message);
            return response.toString();
        }

        // TODO only return authorized events via auth token
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnectionManager.getInstance().getConnection();
            stmt = conn.prepareStatement("SELECT EventName, StartDate, EndDate, TournamentId FROM Event " +
                                         "WHERE StartDate < ? AND EndDate > ?");
            stmt.setLong(1, endTime.getMillis());
            stmt.setLong(2, startTime.getMillis());
            rs = stmt.executeQuery();

            while(rs.next()) {
                events.put(eventToJson(rs));
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

        APIUtils.appendStatus(response, status, message);
        return response.toString();
    }

    /**
     * Convert an event result from the database into JSON
     * @param rs the result set, pointing at an event record
     * @return The JSON version of the event
     */
    private static JSONObject eventToJson(ResultSet rs) throws SQLException {
        String title = rs.getString(1);
        DateTime start = APIUtils.getUTCDateFromResultSet(rs, 2);
        DateTime end = APIUtils.getUTCDateFromResultSet(rs, 3);
        int tourId = rs.getInt(4);

        JSONObject event = new JSONObject();
        event.put("title", title);
        event.put("start", start.toString());
        event.put("end", end.toString());
        if (tourId > 0) {
            event.put("tournamentID", tourId);
        }

        return event;
    }
}
