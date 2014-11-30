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
 * API to handle single event requests.
 * Created by Doug on 11/29/14.
 */
@Path("/event/{id}")
public class SingleEventAPI {
	
	@GET
    @Produces("application/json")
    public String getEvent(@QueryParam(value = "id") final String eventId) 
	{
        int status = 200;
        String message = "";
        JSONObject response = new JSONObject();
        JSONArray events = new JSONArray();
        JSONArray teams = new JSONArray();
        JSONArray players = new JSONArray();
        response.put("events", events);
        response.put("teams", teams);
        response.put("players", players);

        if (eventId == null || eventId.isEmpty()) {
            status = 400;
            message = "You must specify an event id.";
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
                                         "WHERE Id = " + eventId);
            rs = stmt.executeQuery();

            while(rs.next()) {
                events.put(eventToJson(rs));
            }
            conn.prepareStatement("select t1.TeamId, t1.TeamName, t1.TeamOwner from Team t1 join TeamEvent te where te.EventId = " + eventId + " and te.TeamId = t1.TeamId;");
            rs = stmt.executeQuery();
            while(rs.next()) {
            	teams.put(teamToJson(rs));
            }
            stmt = conn.prepareStatement("select p.Login, p.FirstName, p.LastName from PlayerEvent pe, Player p where pe.EventId = " + eventId + " and pe.Login = p.Login;");
            rs = stmt.executeQuery();
            while (rs.next()) {
            	players.put(playerToJson(rs));	
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve event. SQL error.";
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
    
    /**
     * Convert an team result from the database into JSON
     * @param rs the result set, pointing at an team record
     * @return The JSON version of the team
     */
    private static JSONObject teamToJson(ResultSet rs) throws SQLException {
    	int teamId = rs.getInt(1);
    	String teamName = rs.getString(2);
    	String teamOwner = rs.getString(3);
    	JSONObject team = new JSONObject();
    	team.put("teamId", teamId);
    	team.put("teamName", teamName);
    	team.put("teamOwner", teamOwner);
    	
    	return team;
    
    }
    
    /**
     * Convert an player result from the database into JSON
     * @param rs the result set, pointing at an player record
     * @return The JSON version of the player
     */
    private static JSONObject playerToJson(ResultSet rs) throws SQLException {
    	String login = rs.getString(1);
    	String firstName = rs.getString(2);
    	String lastName = rs.getString(3);
    	JSONObject player = new JSONObject();
    	player.put("login", login);
    	player.put("firstName", firstName);
    	player.put("lastName", lastName);
    	return player;
    }
}
