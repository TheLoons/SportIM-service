package org.sportim.service;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sportim.service.beans.EventBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.ConnectionManager;

import javax.ws.rs.*;

import java.sql.*;
import java.util.List;

/**
 * API to handle single event requests.
 * Created by Doug on 11/29/14.
 */
@Path("/event")
public class SingleEventAPI {
	
	@GET
    @Path("{id}")
    @Produces("application/json")
    public String getEvent(@PathParam("id") final int id)
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

        // TODO only return authorized events via auth token
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnectionManager.getInstance().getConnection();
            stmt = conn.prepareStatement("SELECT EventName, StartDate, EndDate, TournamentId FROM Event " +
                                         "WHERE EventId = ?");
            stmt.setInt(1, id);
            rs = stmt.executeQuery();

            while(rs.next()) {
                events.put(eventToJson(rs));
            }
            APIUtils.closeResource(stmt);

            stmt = conn.prepareStatement("select t1.TeamId, t1.TeamName, t1.TeamOwner from Team t1 join TeamEvent te where te.EventId = ? and te.TeamId = t1.TeamId");
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            while(rs.next()) {
            	teams.put(teamToJson(rs));
            }
            APIUtils.closeResource(stmt);

            stmt = conn.prepareStatement("select p.Login, p.FirstName, p.LastName from PlayerEvent pe, Player p where pe.EventId = ? and pe.Login = p.Login");
            stmt.setInt(1, id);
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

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public String createEvent(EventBean event) {
        JSONObject response = new JSONObject();
        int status = 200;
        String message = event.validate();
        if (!message.isEmpty()) {
            APIUtils.appendStatus(response, 400, message);
            return response.toString();
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnectionManager.getInstance().getConnection();

            // first, check for existence of any teams or players
            List<Integer> teams = event.getTeamIDs();
            List<String> players = event.getPlayerIDs();

            if (teams != null && !teams.isEmpty()) {
                if (!verifyTeams(teams, conn)) {
                    status = 422;
                    message = "Non-existent team ID specified.";
                }
            }

            if (status == 200 && players != null && !players.isEmpty()) {
                if (!verifyPlayers(players, conn)) {
                    status = 422;
                    message = "Non-existent player ID specified.";
                }
            }

            if (status == 200 && event.getTournamentID() != -1) {
                if (!verifyTournament(event.getTournamentID(), conn)) {
                    status = 422;
                    message = "Non-existent tournament ID specified.";
                }
            }

            // now, create the event and add any lookups
            conn.setAutoCommit(false);
            int eventID = -1;
            if (status == 200) {
                eventID = addEvent(event, conn);
                if (eventID == -1) {
                    status = 500;
                    message = "Unable to add event. SQL error.";
                }
            }

            // add lookups
            if (status == 200 && teams != null && !teams.isEmpty()) {
                stmt = conn.prepareStatement("INSERT INTO TeamEvent (TeamId, EventId) VALUES (?,?)");

                for (int teamID : teams) {
                    stmt.setInt(1, teamID);
                    stmt.setInt(2, eventID);
                    stmt.addBatch();
                }

                int[] counts = stmt.executeBatch();
                APIUtils.closeResource(stmt);
                for (int i = 0; i < counts.length; i++) {
                    if (counts[i] != 1) {
                        status = 500;
                        message = "Unable to add team lookups.";
                        break;
                    }
                }
            }

            if (status == 200 && players != null && !players.isEmpty()) {
                stmt = conn.prepareStatement("INSERT INTO PlayerEvent (Login, EventId) VALUES (?,?)");

                for (String login : players) {
                    stmt.setString(1, login);
                    stmt.setInt(2, eventID);
                    stmt.addBatch();
                }

                int[] counts = stmt.executeBatch();
                APIUtils.closeResource(stmt);
                for (int i = 0; i < counts.length; i++) {
                    if (counts[i] != 1) {
                        status = 500;
                        message = "Unable to add player lookups.";
                        break;
                    }
                }
            }

            if (status == 200) {
                conn.commit();
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to add event. SQL error.";
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

    private boolean verifyTeams(List<Integer> teams, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(TeamId) FROM Team WHERE TeamId IN (" +
                generateArgs(teams.size()) + ")");
        for (int i = 1; i <= teams.size(); i++) {
            stmt.setInt(i, teams.get(i - 1));
        }
        ResultSet rs = stmt.executeQuery();
        boolean res = true;
        if (rs.next() && rs.getInt(1) != teams.size()) {
            res = false;
        }
        APIUtils.closeResource(rs);
        APIUtils.closeResource(stmt);
        return res;
    }

    private boolean verifyPlayers(List<String> players, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(Login) FROM Player WHERE Login IN(" +
                generateArgs(players.size()) + ")");
        for (int i = 0; i <= players.size(); i++) {
            stmt.setString(i, players.get(i - 1));
        }
        ResultSet rs = stmt.executeQuery();
        boolean res = true;
        if (rs.next() && rs.getInt(1) != players.size()) {
            res = false;
        }
        APIUtils.closeResource(rs);
        APIUtils.closeResource(stmt);
        return res;
    }

    private boolean verifyTournament(int tournamentID, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(TournamentId) FROM Tournament WHERE TournamentId = ?");
        stmt.setInt(1, tournamentID);
        ResultSet rs = stmt.executeQuery();
        boolean res = true;
        if (rs.next() && rs.getInt(1) != 1) {
            res = false;
        }
        APIUtils.closeResource(rs);
        APIUtils.closeResource(stmt);
        return res;
    }

    private int addEvent(EventBean event, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO Event (EventName, StartDate, EndDate, TournamentId) " +
                "VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, event.getTitle());
        stmt.setLong(2, event.getStartMillis());
        stmt.setLong(3, event.getEndMillis());
        if (event.getTournamentID() != -1) {
            stmt.setInt(4, event.getTournamentID());
        }
        else {
            stmt.setNull(4, Types.INTEGER);
        }
        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();

        int id = -1;
        if (rs.next()) {
            return rs.getInt(1);
        }
        APIUtils.closeResource(rs);
        APIUtils.closeResource(stmt);
        return id;
    }

    private static String generateArgs(int count) {
        String args = "?";

        for (int i = 1; i < count; i++) {
            args += ",?";
        }

        return args;
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
    	int teamId = rs.getInt("t1.TeamId");
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
