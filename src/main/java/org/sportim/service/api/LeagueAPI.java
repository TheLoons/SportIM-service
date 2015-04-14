package org.sportim.service.api;

import org.sportim.service.beans.*;
import org.sportim.service.soccer.SoccerTableAPI;
import org.sportim.service.soccer.beans.SoccerTeamResultsBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.*;
import java.util.*;

/**
 * API for league management
 */
@Path("/league")
public class LeagueAPI {
    private ConnectionProvider provider;

    public LeagueAPI() {
        provider = ConnectionManager.getInstance();
    }

    public LeagueAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

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
            // TODO log
            e.printStackTrace();
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
            // TODO log4j 2 log this
            e.printStackTrace();
        } catch (NullPointerException e) {
            status = 500;
            message = "Unable to connect to datasource.";
            // TODO log4j 2 log this
            e.printStackTrace();
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
            // TODO log4j 2 log this
            e.printStackTrace();
        } catch (NullPointerException e) {
            status = 500;
            message = "Unable to connect to datasource.";
            // TODO log4j 2 log this
            e.printStackTrace();
        } finally {
            APIUtils.closeResource(conn);
        }

        ResponseBean resp = new ResponseBean(status, message);
        resp.setId(leagueID);
        return resp;
    }

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
            if (status == 200)
            {
                if(!verifyTeam(teamId, conn))
                {
                    message = "Team Not Found";
                    status = 404;
                }
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
            // TODO log4j 2 log this
            e.printStackTrace();
        } catch (NullPointerException e) {
            status = 500;
            message = "Unable to connect to datasource.";
            // TODO log4j 2 log this
            e.printStackTrace();
        } finally {
            boolean ok = APIUtils.closeResource(stmt);
            ok = ok && APIUtils.closeResource(conn);
            if (!ok) {
                // TODO implement Log4j 2 and log out error
            }
        }
        return new ResponseBean(status, message);
    }

    @PUT
    @Path("{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateLeague(LeagueBean league, @PathParam("id") final int id,
                                     @HeaderParam("token") final String token) {
        league.setId(id);
        return updateLeague(league, token);
    }

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
        String message = "";

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
            // TODO log
            e.printStackTrace();
            status = 500;
            message = "Unable to update league. SQL Error.";
        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }

        return new ResponseBean(status, message);
    }

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
            // TODO log actual error
            e.printStackTrace();
            status = 500;
            message = "Unable to delete league. SQL Error.";
        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return new ResponseBean(status, message);
    }

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
            e.printStackTrace();
            status = 500;
            message =  "Unable to delete Team from League. SQL Error";

        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return new ResponseBean(status, message);
    }

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
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        if (!ok) {
            return new ResponseBean(500, "Unable to add league schedule. Make sure the league and tournament exist.");
        }
        return new ResponseBean(200, "");
    }

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
            // TODO log
            e.printStackTrace();
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
            resp.setTournamentResults(new TreeSet<SoccerTeamResultsBean>());
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
                sport = "other";
            }
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
            sport = null;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (sport == null) {
            return new ResponseBean(500, "Unable to get league table results.");
        }

        SortedSet<SoccerTeamResultsBean> table;
        if (sport.equals("soccer")) {
            table = (new SoccerTableAPI(provider)).getTableForEvents(events);
        } else {
            return new ResponseBean(400, "Unable to get table results for a league with an unsupported sport.");
        }

        if (table == null) {
            return new ResponseBean(500, "Error retrieving table results.");
        }

        ResponseBean resp = new ResponseBean(200, "");
        resp.setTournamentResults(table);
        return resp;
    }

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
            // TODO log
            e.printStackTrace();
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
        stmt.setString(3, league.getSport());
        stmt.setInt(4, league.getId());
        stmt.addBatch();
        stmts.add(stmt);
        return stmts;
    }

    private static boolean verifyTeam(int teamId, Connection conn) throws SQLException
    {
        boolean res = true;
        if(teamId > 0)
        {
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(TeamId) FROM Team Where TeamId = ?");
            stmt.setInt(1, teamId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next() && rs.getInt(1) != 1)
            {
                res = true;
            }
        }
        return res;
    }

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
        stmt.setString(3, league.getSport());

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

