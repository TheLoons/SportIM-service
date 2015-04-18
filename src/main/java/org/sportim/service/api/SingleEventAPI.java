package org.sportim.service.api;

import org.apache.log4j.Logger;
import org.sportim.service.beans.*;
import org.sportim.service.util.*;

import javax.ws.rs.*;

import java.sql.*;
import java.util.*;

/**
 * API to handle single event requests.
 * Created by Doug on 11/29/14.
 */
@Path("/event")
public class SingleEventAPI {
    private static Logger logger = Logger.getLogger(SingleEventAPI.class.getName());
    private ConnectionProvider provider;

    public SingleEventAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SingleEventAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @GET
    @Path("{id}")
    @Produces("application/json")
    public ResponseBean getEvent(@PathParam("id") final int id, @HeaderParam("token") final String token)
	{
        if (!PrivilegeUtil.hasEventView(token, id)) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        EventBean event = null;
        List<TeamBean> teams = new LinkedList<TeamBean>();
        List<UserBean> players = new LinkedList<UserBean>();
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT e.EventName, e.StartDate, e.EndDate, e.TournamentId, e.EventId, " +
                                         "e.EventOwner, e.NextEventID, e.Location, e.EventType, t.Sport " +
                                         "FROM Event e LEFT OUTER JOIN TeamEvent te ON e.EventId = te.EventId " +
                                         "LEFT OUTER JOIN Team t ON te.TeamId = t.TeamId " +
                                         "WHERE e.EventId = ?");
            stmt.setInt(1, id);
            rs = stmt.executeQuery();

            if(rs.next()) {
                event = new EventBean(rs);
                event.setLocation(rs.getString("e.Location"));
                event.setType(rs.getString("e.EventType"));
                event.setSport(SportType.fromString(rs.getString("t.Sport")));
                if (PrivilegeUtil.hasEventUpdate(token, id)) {
                    event.setEditable(true);
                }
            }
            else {
                status = 404;
                message = "Event not found";
            }
            APIUtils.closeResource(stmt);

            stmt = conn.prepareStatement("select t1.TeamId, t1.TeamName, t1.TeamOwner, t1.Sport from Team t1 join TeamEvent te where te.EventId = ? and te.TeamId = t1.TeamId");
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            while(rs.next()) {
            	teams.add(new TeamBean(rs));
            }
            APIUtils.closeResource(stmt);

            stmt = conn.prepareStatement("select p.Login, p.FirstName, p.LastName from PlayerEvent pe, Player p where pe.EventId = ? and pe.Login = p.Login");
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            while (rs.next()) {
            	players.add(new UserBean(rs));
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve event. SQL error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } catch (NullPointerException e) {
            status = 500;
            message = "Unable to connect to datasource.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResource(rs);
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }

        ResponseBean resp = new ResponseBean(status, message);
        if (event != null) {
            event.setTeams(teams);
            event.setPlayers(players);
            resp.setEvent(event);
        } else {
            StatusBean s = new StatusBean(404, "Event not found.");
            resp.setStatus(s);
        }
        return resp;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean createEvent(EventBean event, @HeaderParam("token") final String token) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return new ResponseBean(401, "Not authorized");
        }
        event.setOwner(user);
        return createDBEvent(event, provider);
    }

    @PUT
    @Path("{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateEvent(EventBean event, @PathParam("id") final int id,
                                    @HeaderParam("token") final String token) {
        event.setId(id);
        return updateEvent(event, token);
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateEvent(EventBean event, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasEventUpdate(token, event.getId())) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";

        if (event.getId() < 1) {
            status = 400;
            message = "Invalid event ID.";
            return new ResponseBean(status, message);
        }

        if (!(message = event.validate()).isEmpty()) {
            status = 400;
            return new ResponseBean(status, message);
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();

            // first, check for existence of any teams or players
            if ((message = verifyEventComponents(event, conn)) != null) {
                status = 422;
            }

            if (status == 200) {
                List<PreparedStatement> queries = createUpdateQueries(event, conn);
                for (PreparedStatement s : queries) {
                    // set stmt to s so we can close it in the catch if we hit an exception
                    stmt = s;
                    s.executeBatch();
                }
            }

        } catch (SQLException e) {
            status = 500;
            message = "Unable to update event. SQL Error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }

        return new ResponseBean(status, message);
    }

    @DELETE
    @Path("{id}")
    @Produces("application/json")
    public ResponseBean deleteEvent(@PathParam("id") final int id, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasEventUpdate(token, id)) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("DELETE FROM Event WHERE EventId = ?");
            stmt.setInt(1, id);
            int res = stmt.executeUpdate();
            if (res < 1) {
                status = 404;
                message = "Event with ID " + id + " does not exist.";
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to delete event. SQL Error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return new ResponseBean(status, message);
    }

    /**
     * Create a new event in the database
     * @param event event to create
     * @return a JSON response bean
     */
    public static ResponseBean createDBEvent(EventBean event, ConnectionProvider provider) {
        int status = 200;
        String message = event.validate();
        if (!message.isEmpty()) {
            return new ResponseBean(400, message);
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        int eventID = -1;
        try {
            conn = provider.getConnection();

            // first, check for existence of any teams or players
            if ((message = verifyEventComponents(event, conn)) != null) {
                status = 422;
            }

            // now, create the event and add any lookups
            conn.setAutoCommit(false);
            if (status == 200) {
                eventID = addEvent(event, conn);
                if (eventID == -1) {
                    status = 500;
                    message = "Unable to add event. SQL error.";
                }
            }

            // add lookups
            List<Integer> teams = event.getTeamIDs();
            List<String> players = event.getPlayerIDs();
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
            } else {
                conn.rollback();
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to add event. SQL error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } catch (NullPointerException e) {
            status = 500;
            message = "Unable to connect to datasource.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.setAutoCommit(conn, true);
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }

        ResponseBean resp = new ResponseBean(status, message);
        resp.setId(eventID);
        return resp;
    }

    /**
     * Create the update queries based on an event bean
     * @param event event to update
     * @return set of queries mapped to whether or not the query is a batch
     */
    private List<PreparedStatement> createUpdateQueries(EventBean event, Connection conn) throws SQLException {
        List<PreparedStatement> stmts = new LinkedList<PreparedStatement>();

        // update event stmt
        PreparedStatement stmt;
        if (event.getOwner() != null) {
            stmt = conn.prepareStatement("UPDATE Event " +
                    "SET EventName = ?, StartDate = ?, EndDate = ?, TournamentId = ?, NextEventId = ?, Location = ?, EventType = ?, EventOwner = ? " +
                    "WHERE EventId = ?");
        } else {
            stmt = conn.prepareStatement("UPDATE Event " +
                    "SET EventName = ?, StartDate = ?, EndDate = ?, TournamentId = ?, NextEventId = ?, Location = ?, EventType = ? " +
                    "WHERE EventId = ?");
        }
        stmt.setString(1, event.getTitle());
        stmt.setLong(2, event.getStartMillis());
        stmt.setLong(3, event.getEndMillis());
        if (event.getTournamentID() > 0) {
            stmt.setInt(4, event.getTournamentID());
        } else {
            stmt.setNull(4, Types.INTEGER);
        }
        if (event.getNextEventID() > 0) {
            stmt.setInt(5, event.getNextEventID());
        } else {
            stmt.setNull(5, Types.INTEGER);
        }
        stmt.setString(6, event.getLocation());
        stmt.setString(7, event.getType());
        if (event.getOwner() != null) {
            stmt.setString(8, event.getOwner());
            stmt.setInt(9, event.getId());
        } else {
            stmt.setInt(8, event.getId());
        }

        stmt.addBatch();
        stmts.add(stmt);

        // remove teams and players stmt
        stmt = conn.prepareStatement("DELETE FROM PlayerEvent WHERE EventId = ?");
        stmt.setInt(1, event.getId());
        stmts.add(stmt);
        stmt = conn.prepareStatement("DELETE FROM TeamEvent WHERE EventId = ?");
        stmt.setInt(1, event.getId());
        stmt.addBatch();
        stmts.add(stmt);

        // add teams and players stmt
        if (event.getTeamIDs() != null && !event.getTeamIDs().isEmpty()) {
            stmt = conn.prepareStatement("INSERT INTO TeamEvent (TeamId, EventId) VALUES (?,?)");
            for (int id : event.getTeamIDs()) {
                stmt.setInt(1, id);
                stmt.setInt(2, event.getId());
                stmt.addBatch();
            }
            stmts.add(stmt);
        }

        if (event.getPlayerIDs() != null && !event.getPlayerIDs().isEmpty()) {
            stmt = conn.prepareStatement("INSERT INTO PlayerEvent (Login, EventId) VALUES (?,?)");
            for (String id : event.getPlayerIDs()) {
                stmt.setString(1, id);
                stmt.setInt(2, event.getId());
                stmt.addBatch();
            }
            stmts.add(stmt);
        }

        return stmts;
    }

    private static String verifyEventComponents(EventBean event, Connection conn) throws SQLException {
        // first, check for existence of any teams or players
        List<Integer> teams = event.getTeamIDs();
        List<String> players = event.getPlayerIDs();

        String message = null;
        int status = 200;
        if (teams != null && !teams.isEmpty()) {
            if (!verifyTeams(teams, conn)) {
                status = 422;
                message = "Non-existent team ID specified.";
            }
        }

        if (status == 200 && players != null && !players.isEmpty()) {
            if (event.getOwner() != null) {
                players.add(event.getOwner());
            }
            if (!verifyPlayers(players, conn)) {
                status = 422;
                message = "Non-existent player ID specified.";
            }
        }

        if (status == 200 && event.getTournamentID() > 0) {
            if (!verifyTournament(event.getTournamentID(), conn)) {
                message = "Non-existent tournament ID specified.";
            }
        }
        return message;
    }

    /**
     * Make sure the teams exist in the database
     * @param teams teams to verify
     * @param conn connection to use
     * @return true if all teams exist
     * @throws SQLException
     */
    private static boolean verifyTeams(List<Integer> teams, Connection conn) throws SQLException {
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

    /**
     * Make sure the players exist in the database
     * @param players players to validate
     * @param conn connection to use
     * @return true if all players exist
     * @throws SQLException
     */
    private static boolean verifyPlayers(List<String> players, Connection conn) throws SQLException {
         PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(Login) FROM Player WHERE Login IN(" +
                generateArgs(players.size()) + ")");
        for (int i = 1; i <= players.size(); i++) {
            stmt.setString(i, players.get(i - 1));
        }
        ResultSet rs = stmt.executeQuery();
        boolean res = true;

        if (rs.next() && rs.getInt(1) != players.size()) {

            String temp = rs.getInt(1) + "\t\t" + stmt.toString();
            res = false;
        }
        APIUtils.closeResource(rs);
        APIUtils.closeResource(stmt);
        return res;
    }

    /**
     * Make sure the tournament exists in the database
     * @param tournamentID tournament ID to check
     * @param conn connection to use
     * @return true if the tournament exists
     * @throws SQLException
     */
    private static boolean verifyTournament(int tournamentID, Connection conn) throws SQLException {
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

    /**
     * Add an event to the event table
     * @param event event to add
     * @param conn connection to use
     * @return the auto-generated event ID
     * @throws SQLException
     */
    private static int addEvent(EventBean event, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO Event (EventName, StartDate, EndDate, TournamentId, " +
                "EventOwner, NextEventId, Location, EventType) VALUES (?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, event.getTitle());
        stmt.setLong(2, event.getStartMillis());
        stmt.setLong(3, event.getEndMillis());
        if (event.getTournamentID() > 0) {
            stmt.setInt(4, event.getTournamentID());
        }
        else {
            stmt.setNull(4, Types.INTEGER);
        }
        stmt.setString(5, event.getOwner());
        if (event.getNextEventID() > 0) {
            stmt.setInt(6, event.getNextEventID());
        } else {
            stmt.setNull(6, Types.INTEGER);
        }
        stmt.setString(7, event.getLocation());
        stmt.setString(8, event.getType());
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

    public static boolean updateNextEventId(int eventID, int nextEventID, ConnectionProvider provider) {
        PreparedStatement stmt = null;
        Connection conn = null;
        boolean ok = true;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("UPDATE Event SET NextEventId = ? WHERE EventId = ?");
            if (nextEventID > 0) {
                stmt.setInt(1, nextEventID);
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setInt(2, eventID);
            ok = stmt.executeUpdate() > 0;
        } catch (Exception e) {
            ok = false;
        } finally {
            APIUtils.closeResources(stmt, conn);
        }
        return ok;
    }

    private static String generateArgs(int count) {
        String args = "?";

        for (int i = 1; i < count; i++) {
            args += ",?";
        }

        return args;
    }
}
