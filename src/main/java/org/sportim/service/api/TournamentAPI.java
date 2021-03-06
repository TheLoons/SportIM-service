package org.sportim.service.api;

import org.apache.log4j.Logger;
import org.sportim.service.beans.*;
import org.sportim.service.util.*;

import javax.ws.rs.*;

import java.sql.*;
import java.util.*;

@Path("/tournament")
public class TournamentAPI {
    private static Logger logger = Logger.getLogger(TournamentAPI.class.getName());
    private ConnectionProvider provider;

    public TournamentAPI() {
        provider = ConnectionManager.getInstance();
    }

    public TournamentAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    /**
     * GET request for getting a tournament
     * @param tournamentId
     * @param token
     * @return
     */
    @GET
    @Path("{id}")
    @Produces("application/json")
    public ResponseBean getTournament(@PathParam("id") final int tournamentId, @HeaderParam("token") final String token)
    {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }
        int status = 200;
        String message = "";
        TournamentBean tournament = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT TournamentId, TournamentName, LeagueId, Description FROM Tournament " +
                    "WHERE TournamentId = ?");
            stmt.setInt(1, tournamentId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                tournament = new TournamentBean(rs);
            }

            if (tournament != null) {
                tournament.setEvents(getFullEventsForTournament(tournamentId));
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve tournament. SQL error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } catch (NullPointerException e) {
            status = 500;
            message = "Unable to connect to datasource.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        ResponseBean resp = new ResponseBean(status, message);
        if (tournament != null) {
            resp.setTournament(tournament);
        } else {
            StatusBean s = new StatusBean(404, "Tournament not found.");
            resp.setStatus(s);
        }
        return resp;
    }

    /**
     * Helper metho for getting the full event list for a particular tournament
     * @param tournamentId
     * @return List of Events in Event Beans
     * @throws SQLException
     */
    public List<EventBean> getFullEventsForTournament(final int tournamentId) throws SQLException {
        Connection conn = provider.getConnection();
        // get events
        PreparedStatement stmt = conn.prepareStatement("SELECT e.EventId, e.EventName, e.StartDate, e.EndDate, e.EventOwner, e.NextEventId " +
                "FROM Event e WHERE e.TournamentId = ?");
        stmt.setInt(1, tournamentId);
        ResultSet rs = stmt.executeQuery();
        List<EventBean> events = new ArrayList<EventBean>();
        while (rs.next()) {
            EventBean event = new EventBean();
            event.setId(rs.getInt(1));
            event.setTitle(rs.getString(2));
            event.setStartMillis(rs.getLong(3));
            event.setEndMillis(rs.getLong(4));
            event.setOwner(rs.getString(5));
            event.setNextEventID(rs.getInt(6));
            events.add(event);
        }
        APIUtils.closeResources(rs, stmt);

        // get teams for the events
        for (EventBean event : events) {
            event.setTeams(new ArrayList<TeamBean>());
            Set<Integer> teams = new HashSet<Integer>();
            stmt = conn.prepareStatement("SELECT t.TeamId, t.TeamName " +
                    "FROM Event e INNER JOIN TeamEvent te ON te.EventId = e.EventId INNER JOIN Team t ON te.TeamId = t.TeamId " +
                    "WHERE e.EventId = ?");
            stmt.setInt(1, event.getId());
            rs = stmt.executeQuery();
            while (rs.next()) {
                int teamID = rs.getInt(1);
                if (!teams.contains(teamID)) {
                    teams.add(teamID);
                    TeamBean team = new TeamBean();
                    team.setId(teamID);
                    team.setName(rs.getString(2));
                    event.getTeams().add(team);
                }
            }
        }
        APIUtils.closeResources(rs, stmt, conn);

        return events;
    }

    /**
     * Method for returning event IDs for a given tournament
     * @param tournamentID
     * @return
     */
    public List<Integer> getEventsForTournament(final int tournamentID) {
        List<Integer> events = new ArrayList<Integer>();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT EventId FROM Event WHERE TournamentId = ?");
            stmt.setInt(1, tournamentID);
            rs = stmt.executeQuery();
            while (rs.next()) {
                events.add(rs.getInt(1));
            }
        } catch (Exception e) {
            logger.error("Unable to get events for tournament: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            events = null;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
        return events;
    }

    /**
     * Create a tournament
     * @param tournament
     * @param token
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean createTournament(TournamentBean tournament, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasLeagueUpdate(token, tournament.getLeagueId())) {
            return new ResponseBean(401, "Not authorized");
        }
        return createDBTournament(tournament);
    }

    /**
     * Create a new tournament in the database
     * @param tournament
     * @return a JSON response bean
     */
    public ResponseBean createDBTournament(TournamentBean tournament) {
        int status = 200;
        String message = tournament.validate();
        if (!message.isEmpty()) {
            return new ResponseBean(400, message);
        }

        Connection conn = null;
        int tournamentID = -1;
        try {
            conn = provider.getConnection();

            // first, check for League
            if ((message = verifyTournamentComponents(tournament, conn)) != null) {
                status = 422;
            }

            // now, create the tournament
            conn.setAutoCommit(false);
            if (status == 200) {
                tournamentID = addTournament(tournament, conn);
                if (tournamentID == -1) {
                    status = 500;
                    message = "Unable to add tournament. SQL error.";
                }
            }
            if (status == 200) {
                conn.commit();
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to add tournament. SQL error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } catch (NullPointerException e) {
            status = 500;
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResource(conn);
        }

        ResponseBean resp = new ResponseBean(status, message);
        resp.setId(tournamentID);
        return resp;
    }

    /**
     * Update a tournament
     * @param tournament
     * @param id
     * @param token
     * @return
     */
    @PUT
    @Path("{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateTournament(TournamentBean tournament, @PathParam("id") final int id,
                                         @HeaderParam("token") final String token) {
        tournament.setTournamentId(id);
        return updateTournament(tournament, token);
    }

    /**
     * Update a tournament with a Tournament Bean
     * @param tournament
     * @param token
     * @return
     */
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateTournament(TournamentBean tournament, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasTournamentUpdate(token, tournament.getTournamentId())) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";

        if (tournament.getTournamentId() < 1) {
            status = 400;
            message = "Invalid tournament ID.";
            return new ResponseBean(status, message);
        }

        if (!(message = tournament.validate()).isEmpty()) {
            status = 400;
            return new ResponseBean(status, message);
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();

            // first, check for existence of any teams or players
            if ((message = verifyTournamentComponents(tournament, conn)) != null) {
                status = 422;
            }

            if (status == 200) {
                List<PreparedStatement> queries = createUpdateQueries(tournament, conn);
                for (PreparedStatement s : queries) {
                    // set stmt to s so we can close it in the catch if we hit an exception
                    stmt = s;
                    s.executeBatch();
                }
            }

        } catch (SQLException e) {
            status = 500;
            message = "Unable to update tournament. SQL Error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        return new ResponseBean(status, message);
    }

    /**
     * Delete a tournament
     * @param id
     * @param token
     * @return
     */
    @DELETE
    @Path("{id}")
    @Produces("application/json")
    public ResponseBean deleteTournament(@PathParam("id") final int id, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasTournamentUpdate(token, id)) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("DELETE FROM Tournament WHERE TournamentId = ?");
            stmt.setInt(1, id);
            int res = stmt.executeUpdate();
            if (res < 1) {
                status = 404;
                message = "Tournament with ID " + id + " does not exist.";
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to delete tournament. SQL Error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }
        return new ResponseBean(status, message);
    }

    /**
     * Create the update queries based on a tournament bean
     * @param tournament
     * @return set of queries mapped to whether or not the query is a batch
     */
    private List<PreparedStatement> createUpdateQueries(TournamentBean tournament, Connection conn) throws SQLException {
        List<PreparedStatement> stmts = new LinkedList<PreparedStatement>();

        // update tournament stmt
        PreparedStatement stmt = conn.prepareStatement("UPDATE Tournament " +
                "SET TournamentName = ?, LeagueId = ?, Description = ?" +
                "WHERE TournamentId = ?");
        stmt.setString(1, tournament.getTournamentName());
        stmt.setInt(2, tournament.getLeagueId());
        stmt.setString(3, tournament.getDesc());
        stmt.setInt(4, tournament.getTournamentId());
        stmt.addBatch();
        stmts.add(stmt);
        return stmts;
    }

    /**
     * Verify tournament components prior to building/referencing a tournament
     * @param tournament
     * @param conn
     * @return
     * @throws SQLException
     */
    private static String verifyTournamentComponents(TournamentBean tournament, Connection conn) throws SQLException {

        String message = null;
        if (tournament.getLeagueId() > 0) {
            if (!verifyLeague(tournament.getLeagueId(), conn)) {
                message = "Non-existent league ID specified.";
            }
        }
        return message;
    }

    /**
     * Make sure the league exists in the database
     * @param leagueID
     * @param conn
     * @return true if the league exists
     * @throws SQLException
     */
    private static boolean verifyLeague(int leagueID, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(LeagueId) FROM League WHERE LeagueId = ?");
        stmt.setInt(1, leagueID);
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
     * Add an tournament to the tournament table
     * @param tournament
     * @param conn
     * @return the auto-generated tournament ID
     * @throws SQLException
     */
    private static int addTournament(TournamentBean tournament, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO Tournament (TournamentName, LeagueId, Description) " +
                "VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, tournament.getTournamentName());
        stmt.setInt(2, tournament.getLeagueId());
        stmt.setString(3, tournament.getDesc());
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



}
