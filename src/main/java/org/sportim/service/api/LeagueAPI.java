package org.sportim.service.api;

import org.apache.log4j.Logger;
import org.sportim.service.beans.*;
import org.sportim.service.beans.stats.AbstractTeamResultsBean;
import org.sportim.service.util.SportType;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.*;
import java.util.*;

/**
 * API for league management
 */
@Path("/league")
public class LeagueAPI {
    private static Logger logger = Logger.getLogger(LeagueAPI.class.getName());
    private ConnectionProvider provider;
    private StatAPIMapper apiMapper;

    public LeagueAPI() {
        provider = ConnectionManager.getInstance();
        apiMapper = new StatAPIMapper(provider);
    }

    public LeagueAPI(ConnectionProvider provider) {
        this.provider = provider;
        apiMapper = new StatAPIMapper(provider);
    }

    /**
     * GET request for getting the Leagues for a current user
     * @param token
     * @return
     */
    @GET
    @Produces("application/json")
    public ResponseBean getLeaguesForCurrentUser(@HeaderParam("token") final String token) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return new ResponseBean(401, "Not authorized");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int status = 200;
        List<LeagueBean> leagues = new LinkedList<LeagueBean>();
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT LeagueId, LeagueName, LeagueOwner, Sport FROM League " +
                                         "WHERE LeagueOwner = ?");
            stmt.setString(1, user);
            rs = stmt.executeQuery();
            while (rs.next()) {
                leagues.add(new LeagueBean(rs));
            }
        } catch (Exception e) {
            logger.error("Error getting leagues for current user:" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            status = 500;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (status != 200) {
            return new ResponseBean(status, "Unable to retrieve leagues.");
        }
        ResponseBean resp = new ResponseBean(status, "");
        resp.setLeagues(leagues);
        return resp;
    }

    /**
     * Get a league, given a league ID
     * @param leagueId
     * @param token
     * @return
     */
    @GET
    @Path("{id}")
    @Produces("application/json")
    public ResponseBean getLeague(@PathParam("id") final int leagueId, @HeaderParam("token") final String token)
    {
        // any user can see league info
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";
        LeagueBean league = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT l.LeagueId, l.LeagueName, l.LeagueOwner, l.Sport, t.TeamId, t.TeamName, t.TeamOwner" +
                    " FROM League l LEFT OUTER JOIN TeamBelongsTo tb ON tb.LeagueId = l.LeagueId " +
                    " LEFT OUTER JOIN Team t ON t.TeamId = tb.TeamId " +
                    "WHERE l.LeagueId = ?");
            stmt.setInt(1, leagueId);
            rs = stmt.executeQuery();

            List<TeamBean> teams = new LinkedList<TeamBean>();
            while (rs.next()) {
                if (league == null) {
                    league = new LeagueBean(rs);
                }
                TeamBean team = new TeamBean();
                team.setName(rs.getString(6));
                if (team.getName() != null) {
                    team.setId(rs.getInt(5));
                    team.setOwner(rs.getString(7));
                    teams.add(team);
                }
            }
            if (league != null) {
                league.setTeams(teams);
            }
            APIUtils.closeResource(stmt);
        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve league. SQL error.";
            logger.error("Error getting league:" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } catch (NullPointerException e) {
            status = 500;
            message = "Unable to connect to datasource.";
            logger.error("Unable to connect to datasource:" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResource(rs);
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }

        ResponseBean resp = new ResponseBean(status, message);
        if (league != null) {
            resp.setLeague(league);
        } else if (status != 500) {
            StatusBean s = new StatusBean(404, "League not found.");
            resp.setStatus(s);
        } else {
            StatusBean s = new StatusBean(status, message);
            resp.setStatus(s);
        }
        return resp;
    }

    /**
     * Create a league (redirects to createDBLeague method)
     * @param league
     * @param token
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean createLeague(LeagueBean league, @HeaderParam("token") final String token) {
        // only the to-be owner can create a league
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return new ResponseBean(401, "Not authorized");
        }
        league.setOwner(user);
        return createDBLeague(league);
    }

    /**
     * Create a new league in the database
     * @param league the league to create
     * @return a JSON response bean
     */
    public ResponseBean createDBLeague(LeagueBean league) {
        int status = 200;
        String message = league.validate();
        if (!message.isEmpty()) {
            return new ResponseBean(400, message);
        }

        Connection conn = null;
        int leagueID = -1;
        try {
            conn = provider.getConnection();

            // first, check for League
            if ((message = verifyLeagueComponents(league, conn)) != null) {
                status = 422;
            }

            // now, create the league
            conn.setAutoCommit(false);
            if (status == 200) {
                leagueID = addLeague(league, conn);
                if (leagueID == -1) {
                    status = 500;
                    message = "Unable to add league. SQL error.";
                }
            }
            if (status == 200) {
                conn.commit();
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to add league. SQL error.";
            logger.error("Unable to add league" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } catch (NullPointerException e) {
            status = 500;
            message = "Unable to connect to datasource.";
            logger.error("Unable to add league" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResource(conn);
        }

        ResponseBean resp = new ResponseBean(status, message);
        resp.setId(leagueID);
        return resp;
    }

    /**
     * Put method to add a team to a league
     * @param teamId
     * @param leagueId
     * @param token
     * @return
     */
    @PUT
    @Path("{leagueId}/add")
    @Produces("application/json")
    public ResponseBean addTeamToLeague(@QueryParam("teamId") final int teamId, @PathParam("leagueId") final int leagueId,
                                        @HeaderParam("token") final String token)
    {
        if (!PrivilegeUtil.hasLeagueUpdate(token, leagueId)) {
             return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";
        if(teamId < 1)
        {
            status =  400;
            message = "Invalid team ID";
            return new ResponseBean(status, message);
        }
        if(leagueId < 1)
        {
            status = 400;
            message = "Invalid League Id";
            return new ResponseBean(status, message);
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionManager.getInstance().getConnection();

            // now, create the event and add any lookups
            conn.setAutoCommit(false);
            if(!verifyTeam(teamId, conn))
            {
                message = "Team Not Found";
                status = 404;
            }

            if(status == 200)
            {
                if(!verifyLeague(leagueId, conn))
                {
                    status = 404;
                    message = "League Not Found";
                }
            }
            if(status == 200)
            {
                stmt = conn.prepareStatement("INSERT INTO TeamBelongsTo(TeamID, LeagueId) Values (?, ?)");
                stmt.setInt(1, teamId);
                stmt.setInt(2, leagueId);
                stmt.executeUpdate();

            }
            if (status == 200) {
                conn.commit();
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to add team. SQL error.";
            logger.error("Unable to add team" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } catch (NullPointerException e) {
            status = 500;
            message = "Unable to connect to datasource.";
            logger.error("Unable to add team" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }
        return new ResponseBean(status, message);
    }

    /**
     * Update a league
     * @param league
     * @param id
     * @param token
     * @return
     */
    @PUT
    @Path("{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateLeague(LeagueBean league, @PathParam("id") final int id,
                                     @HeaderParam("token") final String token) {
        league.setId(id);
        return updateLeague(league, token);
    }

    /**
     * Update a league given league bean
     * @param league
     * @param token
     * @return
     */
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateLeague(LeagueBean league, @HeaderParam("token") final String token) {
        String user = AuthenticationUtil.validateToken(token);
        if (!PrivilegeUtil.hasLeagueUpdate(token, league.getId())) {
            return new ResponseBean(401, "Not authorized");
        }

        if (league.getOwner() == null) {
            league.setOwner(user);
        }

        int status = 200;
        String message;

        if (league.getId() < 1) {
            status = 400;
            message = "Invalid league ID.";
            return new ResponseBean(status, message);
        }

        if (!(message = league.validate()).isEmpty()) {
            status = 400;
            return new ResponseBean(status, message);
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();

            // first, check for existence of any teams or players
            if ((message = verifyLeagueComponents(league, conn)) != null) {
                status = 422;
            }

            if (status == 200) {
                List<PreparedStatement> queries = createUpdateQueries(league, conn);
                for (PreparedStatement s : queries) {
                    // set stmt to s so we can close it in the catch if we hit an exception
                    stmt = s;
                    s.executeBatch();
                }
            }

        } catch (SQLException e) {
            logger.error("Unable to update league" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            status = 500;
            message = "Unable to update league. SQL Error.";
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        return new ResponseBean(status, message);
    }

    /**
     * Delete League
     * @param id
     * @param token
     * @return
     */
    @DELETE
    @Path("{id}")
    @Produces("application/json")
    public ResponseBean deleteLeague(@PathParam("id") final int id, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasLeagueUpdate(token, id)) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("DELETE FROM League WHERE LeagueId = ?");
            stmt.setInt(1, id);
            int res = stmt.executeUpdate();
            if (res < 1) {
                status = 404;
                message = "League with ID " + id + " does not exist.";
            }
        } catch (SQLException e) {
            logger.error("Unable to delete league" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            status = 500;
            message = "Unable to delete league. SQL Error.";
        } finally {
            APIUtils.closeResources(stmt, conn);
        }
        return new ResponseBean(status, message);
    }

    /**
     * Remove team from league
     * @param leagueId
     * @param teamId
     * @param token
     * @return
     */
    @DELETE
    @Produces("application/json")
    public ResponseBean removeTeamFromLeague(@QueryParam("leagueId") final int leagueId,
                                             @QueryParam("teamId") final int teamId,
                                             @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasLeagueUpdate(token, leagueId)) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";

        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = ConnectionManager.getInstance().getConnection();
            stmt = conn.prepareStatement("DELETE FROM TeamBelongsTo WHERE LeagueId = ? AND TeamId = ?");
            stmt.setInt(1, leagueId);
            stmt.setInt(2, teamId);
            int res = stmt.executeUpdate();
            if(res < 1)
            {
                status = 404;
                message = "Team " + teamId + " does not belong to League " + leagueId + ".";
            }

        }
        catch (SQLException e)
        {
            status = 500;
            message =  "Unable to delete Team from League. SQL Error";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return new ResponseBean(status, message);
    }

    /**
     * When creating schedule, create league on league table
     * @param table
     * @param leagueId
     * @param token
     * @return
     */
    @POST
    @Path("{id}/table")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean addLeagueTable(TournamentBean table, @PathParam("id") final int leagueId,
                                       @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasLeagueUpdate(token, leagueId)) {
            return new ResponseBean(401, "Not authorized");
        }
        if (leagueId < 1 || table.getTournamentId() < 1 || table.getDesc() == null || table.getDesc().isEmpty()) {
            return new ResponseBean(400, "Bad request");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean ok = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT IGNORE INTO LeagueTable (LeagueId, TournamentId, Description) " +
                    "VALUES (?,?,?)");
            stmt.setInt(1, leagueId);
            stmt.setInt(2, table.getTournamentId());
            stmt.setString(3, table.getDesc());
            ok = stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Unable to add league table" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (!ok) {
            return new ResponseBean(500, "Unable to add league schedule. Make sure the league and tournament exist.");
        }
        return new ResponseBean(200, "");
    }

    /**
     * Get League Table
     * @param leagueId
     * @param token
     * @return
     */
    @GET
    @Path("{id}/table")
    @Produces("application/json")
    public ResponseBean getLeagueTables(@PathParam("id") final int leagueId, @HeaderParam("token") final String token) {
        if ((AuthenticationUtil.validateToken(token)) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean ok = false;
        List<TournamentBean> tables = new ArrayList<TournamentBean>();
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT lt.TournamentId, lt.Description, t.TournamentName " +
                    "FROM LeagueTable lt INNER JOIN Tournament t ON lt.TournamentId = t.TournamentId " +
                    "WHERE lt.LeagueId = ?");
            stmt.setInt(1, leagueId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                TournamentBean t = new TournamentBean();
                t.setLeagueId(leagueId);
                t.setTournamentId(rs.getInt(1));
                t.setDesc(rs.getString(2));
                t.setTournamentName(rs.getString(3));
                tables.add(t);
            }
            ok = true;
        } catch (Exception e) {
            logger.error("Unable to get league tables" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (!ok) {
            return new ResponseBean(500, "Unable to get league tables. Please make sure the league exists.");
        }

        ResponseBean resp = new ResponseBean(200, "");
        resp.setTables(tables);
        return resp;
    }

    /**
     * Get League Results to display on table
     * @param leagueID
     * @param tableID
     * @param token
     * @return
     */
    @GET
    @Path("{leagueID}/table/{tableID}")
    @Produces("application/json")
    public ResponseBean getTableResults(@PathParam("leagueID") final int leagueID, @PathParam("tableID") final int tableID,
                                        @HeaderParam("token") final String token) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        List<Integer> events = (new TournamentAPI(provider)).getEventsForTournament(tableID);
        if (events == null) {
            ResponseBean resp = new ResponseBean(200, "");
            resp.setTournamentResults(new TreeSet<AbstractTeamResultsBean>());
            return resp;
        }

        // Get the table from the correct results plug in
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sport = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT Sport From League WHERE LeagueId = ?");
            stmt.setInt(1, leagueID);
            rs = stmt.executeQuery();
            if (rs.next()) {
                sport = rs.getString(1);
            } else {
                sport = "Unknown";
            }
        } catch (Exception e) {
            logger.error("Unable to get sport for league" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            sport = null;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (sport == null) {
            return new ResponseBean(500, "Unable to get league table results.");
        }

        SortedSet<AbstractTeamResultsBean> table;
        TableAPI api = apiMapper.getTableAPI(SportType.fromString(sport));
        if (api != null) {
            table = api.getTableForEvents(events);
        } else {
            return new ResponseBean(400, "Unable to get table results for a league with an unsupported type.");
        }

        if (table == null) {
            return new ResponseBean(500, "Error retrieving table results.");
        }

        ResponseBean resp = new ResponseBean(200, "");
        resp.setTournamentResults(table);
        return resp;
    }

    /**
     * Delete League Table
     * @param leagueId
     * @param tableId
     * @param token
     * @return
     */
    @DELETE
    @Path("{id}/table")
    @Produces("application/json")
    public ResponseBean deleteLeagueTable(@PathParam("id") final int leagueId, @QueryParam("tableId") final int tableId,
                                          @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasLeagueUpdate(token, leagueId)) {
            return new ResponseBean(401, "Not authorized");
        }
        if (leagueId < 1 || tableId < 1) {
            return new ResponseBean(400, "Bad request");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean ok = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("DELETE FROM LeagueTable WHERE LeagueId = ? AND TournamentId = ?");
            stmt.setInt(1, leagueId);
            stmt.setInt(2, tableId);
            ok = stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Unable to delete league table" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (!ok) {
            return new ResponseBean(500, "Unable to delete league table. Make sure it exists first.");
        }
        return new ResponseBean(200, "");
    }

    /**
     * Create the update queries based on a league bean
     * @param league the league to create queries for
     * @return set of queries mapped to whether or not the query is a batch
     */
    private List<PreparedStatement> createUpdateQueries(LeagueBean league, Connection conn) throws SQLException {
        List<PreparedStatement> stmts = new LinkedList<PreparedStatement>();

        // update league stmt
        PreparedStatement stmt = conn.prepareStatement("UPDATE League " +
                "SET LeagueName = ?, LeagueOwner = ?, Sport = ? " +
                "WHERE LeagueId = ?");
        stmt.setString(1, league.getName());
        stmt.setString(2, league.getOwner());
        stmt.setString(3, league.getSport() != null ? league.getSport().name().toLowerCase() : null);
        stmt.setInt(4, league.getId());
        stmt.addBatch();
        stmts.add(stmt);
        return stmts;
    }

    /**
     * Verify team information, prior to adding to a league
     * @param teamId
     * @param conn
     * @return
     * @throws SQLException
     */
    private static boolean verifyTeam(int teamId, Connection conn) throws SQLException
    {
        boolean res = true;
        if(teamId > 0) {
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(TeamId) FROM Team Where TeamId = ?");
            stmt.setInt(1, teamId);
            ResultSet rs = stmt.executeQuery();
            if(!rs.next() || rs.getInt(1) != 1) {
                res = false;
            }
        }
        return res;
    }

    /**
     * Verify League Components prior to creating/updating a league
     * @param league
     * @param conn
     * @return
     * @throws SQLException
     */
    private static String verifyLeagueComponents(LeagueBean league, Connection conn) throws SQLException {
        String message = null;
        if (league.getId() > 0) {
            if (!verifyLeague(league.getId(), conn)) {
                message = "Non-existent league ID specified.";
            }
            if (!verifyOwner(league.getOwner(), conn)) {
                message = "Non-existent league owner specified.";
            }
        }
        return message;
    }

    /**
     * Make sure the league exists in the database
     * @param leagueID ID of the league to verify
     * @param conn the connection to use
     * @return true if the league exists
     * @throws java.sql.SQLException
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
     * Verify owner exists in system
     * @param owner
     * @param conn
     * @return
     * @throws SQLException
     */
    private static boolean verifyOwner(String owner, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(Login) FROM Player WHERE Login = ?");
        stmt.setString(1, owner);
        ResultSet rs = stmt.executeQuery();
        boolean res = rs.next() && rs.getInt(1) == 1;
        APIUtils.closeResource(rs);
        APIUtils.closeResource(stmt);
        return res;
    }

    /**
     * Add a league to the league table
     * @param league league to add
     * @param conn connection to use
     * @return the auto-generated league ID
     * @throws java.sql.SQLException
     */
    private static int addLeague(LeagueBean league, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO League (LeagueName, LeagueOwner, Sport) " +
                "VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, league.getName());
        stmt.setString(2, league.getOwner());
        stmt.setString(3, league.getSport() != null ? league.getSport().name().toLowerCase() : null);

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

