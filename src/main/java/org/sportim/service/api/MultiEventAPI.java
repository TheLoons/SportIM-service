package org.sportim.service.api;

import org.joda.time.DateTime;
import org.sportim.service.beans.EventBean;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.AuthenticationUtil;
import org.sportim.service.util.ConnectionManager;
import org.sportim.service.util.ConnectionProvider;

import javax.naming.InitialContext;
import javax.ws.rs.*;
import java.sql.*;
import java.util.*;

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
    public ResponseBean batchPostEvents(List<EventBean> events, @HeaderParam("token") final String token) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return new ResponseBean(401, "Not authorized");
        }

        Map<Integer, Integer> origIdToDatabaseId = new HashMap<Integer, Integer>();
        ResponseBean resp = new ResponseBean(200, "");
        List<Integer> ids = new ArrayList<Integer>(events.size());
        for (EventBean event : events) {
            event.setOwner(user);
            ResponseBean sresp = SingleEventAPI.createDBEvent(event, provider);
            if (resp.getStatus().getCode() != 200) {
                return sresp;
            }
            ids.add(sresp.getId());
            origIdToDatabaseId.put(event.getId(), sresp.getId());
        }
        resp.setIds(ids);

        // Now, see if we need to put in bracket info for these events
        for (EventBean event : events) {
            if (event.getNextEventID() < 1) {
                continue;
            }
            int dbId = origIdToDatabaseId.get(event.getId()) != null ? origIdToDatabaseId.get(event.getId()) : 0;
            int nextDbId = origIdToDatabaseId.get(event.getNextEventID()) != null ? origIdToDatabaseId.get(event.getNextEventID()) : 0;
            if (dbId < 1 || nextDbId < 1) {
                continue;
            }
            SingleEventAPI.updateNextEventId(dbId, nextDbId, provider);
        }

        return resp;
    }

    @GET
    @Produces("application/json")
    public ResponseBean getEventsForRange(@QueryParam(value = "start") final String start,
                                          @QueryParam(value = "end") final String end,
                                          @HeaderParam(value = "token") final String token) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return new ResponseBean(401, "Not authorized");
        }

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

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<EventBean> events = new LinkedList<EventBean>();
        try {
            conn = provider.getConnection();
            Set<Integer> teams = getTeamsForUser(user, conn);
            stmt = getEventQuery(teams, conn);
            stmt.setLong(1, endTime.getMillis());
            stmt.setLong(2, startTime.getMillis());
            stmt.setString(3, user);
            stmt.setString(4, user);
            int i = 5;
            for (int team : teams) {
                stmt.setInt(i++, team);
            }
            rs = stmt.executeQuery();

            while(rs.next()) {
                EventBean e = new EventBean(rs);
                e.setLocation(rs.getString("e.Location"));
                e.setType(rs.getString("e.EventType"));
                if (user.equals(e.getOwner())) {
                    e.setEditable(true);
                }
                events.add(e);
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
            APIUtils.closeResources(rs, stmt, conn);
        }

        ResponseBean resp = new ResponseBean(status, message);
        resp.setEvents(events);
        return resp;
    }

    private static PreparedStatement getEventQuery(Set<Integer> teams, Connection conn) throws SQLException {
        String query = "SELECT DISTINCT e.EventName, e.StartDate, e.EndDate, e.TournamentId, e.EventId, e.EventOwner, e.NextEventId, e.Location, e.EventType " +
                        "FROM Event e LEFT OUTER JOIN TeamEvent te ON te.EventId = e.EventId " +
                        "LEFT OUTER JOIN PlayerEvent pe ON e.EventId = pe.EventId " +
                        "WHERE StartDate < ? AND EndDate > ? AND (e.EventOwner = ? OR pe.Login = ?";
        if (!teams.isEmpty()) {
            query += " OR te.TeamId IN(" + params(teams.size()) + ")";
        }
        query += ")";
        return conn.prepareStatement(query);
    }

    private static String params(int numParams) {
        String ret = "";
        for (int i = 0; i < numParams; i++) {
            if (i > 0) {
                ret += ",";
            }
            ret += "?";
        }
        return ret;
    }

    private static Set<Integer> getTeamsForUser(String user, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT TeamId FROM PlaysFor WHERE Login = ?");
        stmt.setString(1, user);
        ResultSet rs = stmt.executeQuery();
        Set<Integer> teams = new HashSet<Integer>();
        while (rs.next()) {
            teams.add(rs.getInt(1));
        }
        APIUtils.closeResources(rs, stmt);
        return teams;
    }
}
