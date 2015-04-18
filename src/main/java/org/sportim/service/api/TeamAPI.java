package org.sportim.service.api;

import org.apache.log4j.Logger;
import org.sportim.service.beans.*;
import org.sportim.service.util.*;

import javax.ws.rs.*;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

/**
 * API for team management.
 */
@Path("/team")
public class TeamAPI {
    private static Logger logger = Logger.getLogger(TeamAPI.class.getName());
    private ConnectionProvider provider;

    public TeamAPI() {
        provider = ConnectionManager.getInstance();
    }

    public TeamAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @GET
    @Path("edit")
    @Produces("application/json")
    public ResponseBean getTeamsForEditing(@HeaderParam("token") final String token) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return new ResponseBean(401, "Not authorized");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int status = 200;
        List<TeamBean> teams = new LinkedList<TeamBean>();
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT t1.TeamId, t1.TeamName, t1.TeamOwner, t1.Sport FROM Team t1 WHERE TeamOwner = ?");
            stmt.setString(1, user);
            rs = stmt.executeQuery();
            while (rs.next()) {
                TeamBean team = new TeamBean(rs);
                teams.add(team);
            }
        } catch (Exception e) {
            logger.error("Error starting getting teams for editing: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            status = 500;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (status != 200) {
            return new ResponseBean(status, "Unable to retrieve teams.");
        }
        ResponseBean resp = new ResponseBean(status, "");
        resp.setTeams(teams);
        return resp;
    }

    @GET
    @Path("view")
    @Produces("application/json")
    public ResponseBean getTeamsForViewing(@HeaderParam("token") final String token) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return new ResponseBean(401, "Not authorized");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int status = 200;
        List<TeamBean> teams = new LinkedList<TeamBean>();
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT DISTINCT t1.TeamId, t1.TeamName, t1.TeamOwner, t1.Sport " +
                    "FROM Team t1 LEFT OUTER JOIN TeamBelongsTo tb ON tb.TeamId = t1.TeamId " +
                    "LEFT OUTER JOIN League l ON l.LeagueId = tb.LeagueId " +
                    "WHERE t1.TeamOwner = ? OR l.LeagueOwner = ?");
            stmt.setString(1, user);
            stmt.setString(2, user);
            rs = stmt.executeQuery();
            while (rs.next()) {
                TeamBean team = new TeamBean(rs);
                teams.add(team);
            }
        } catch (Exception e) {
            logger.error("Error starting getting teams for viewing: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            status = 500;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (status != 200) {
            return new ResponseBean(status, "Unable to retrieve teams.");
        }
        ResponseBean resp = new ResponseBean(status, "");
        resp.setTeams(teams);
        return resp;
    }

    @GET
    @Path("{id}")
    @Produces("application/json")
    public ResponseBean getTeam(@PathParam("id") final int teamId, @HeaderParam("token") final String token)
    {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        List<UserBean> players = new LinkedList<UserBean>();
        String message = "";
        TeamBean team = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT t1.TeamId, t1.TeamName, t1.TeamOwner, t1.Sport FROM Team t1 " +
                    "WHERE t1.TeamId = ?");
            stmt.setInt(1, teamId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                team = new TeamBean(rs);
            }
            else
            {
                status = 404;
                message = "Team not found";
            }

            stmt = conn.prepareStatement("SELECT p1.Login, p1.FirstName, p1.LastName from Player p1, PlaysFor pf WHERE pf.TeamID = ? AND p1.Login = pf.Login");
            stmt.setInt(1, teamId);
            rs = stmt.executeQuery();

            while(rs.next())
            {
                players.add(new UserBean(rs));
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve team. SQL error.";
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
        if (team != null) {
            team.setPlayers(players);
            resp.setTeam(team);
        } else {
            StatusBean s = new StatusBean(404, "Team not found.");
            resp.setStatus(s);
        }
        return resp;
    }

    @GET
    @Path("{id}/colors")
    @Produces("application/json")
    public ResponseBean getTeamColors(@PathParam("id") final int teamId, @HeaderParam("token") final String token)
    {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";
        ColorBean team = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT t1.TeamId, PrimaryColor, SecondaryColor, TertiaryColor FROM TeamColors t1 " +
                    "WHERE t1.TeamId = ?");
            stmt.setInt(1, teamId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                team = new ColorBean(rs);
            }
            else
            {
                status = 404;
                message = "Team Colors not found";
            }

        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve team. SQL error.";
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
        if (team != null) {
            resp.setColors(team);
        } else {
            StatusBean s = new StatusBean(404, "Team not found.");
            resp.setStatus(s);
        }
        return resp;
    }

    @GET
    @Produces("application/json")
    public ResponseBean getTeams(@QueryParam(value="league") final int leagueID, @HeaderParam("token") final String token)
    {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";
        List<TeamBean> teams = new LinkedList<TeamBean>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT t1.TeamId, t1.TeamName, t1.TeamOwner, t1.Sport FROM Team t1, TeamBelongsTo l " +
                    "WHERE l.LeagueId = ? AND t1.TeamId = l.TeamId");
            stmt.setInt(1, leagueID);
            rs = stmt.executeQuery();

            while (rs.next()) {
                teams.add(new TeamBean(rs));
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve teams. SQL error.";
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
        resp.setTeams(teams);
        return resp;
    }

    @POST
    @Path("{id}/colors")
    @Produces("application/json")
    @Consumes("application/json")
    public ResponseBean createColors(ColorBean color, @PathParam("id") final int teamID,
                                     @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasTeamUpdate(token, teamID)) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT IGNORE INTO TeamColors (TeamId, PrimaryColor, SecondaryColor, TertiaryColor) " +
                    "VALUES (?,?,?,?)");
            stmt.setInt(1, teamID);
            stmt.setString(2, color.getPrimaryColor());
            stmt.setString(3, color.getSecondaryColor());
            stmt.setString(4, color.getTertiaryColor());
            int res = stmt.executeUpdate();
            if (res < 1) {
                message = "Color with that team ID already exists.";
                status = 400;
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to add color. SQL error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }

        return new ResponseBean(status, message);
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean createTeam(TeamBean team, @HeaderParam("token") final String token) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return new ResponseBean(401, "Not authorized");
        }
        team.setOwner(user);
        return createDBTeam(team);
    }

    /**
     * Create a new team in the database
     * @param team
     * @return a JSON response bean
     */
    public ResponseBean createDBTeam(TeamBean team) {
        int status = 200;
        String message = team.validate();
        if (!message.isEmpty()) {
            return new ResponseBean(400, message);
        }

        Connection conn = null;
        int teamID = -1;
        try {
            conn = provider.getConnection();

            // first, check for existence of any teams or players
            if ((message = verifyTeamComponents(team, conn)) != null) {
                status = 422;
            }

            // now, create the event and add any lookups
            conn.setAutoCommit(false);
            if (status == 200) {
                teamID = addTeam(team, conn);
                if (teamID == -1) {
                    status = 500;
                    message = "Unable to add team. SQL error.";
                }
            }
            if (status == 200) {
                conn.commit();
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to add team. SQL error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } catch (NullPointerException e) {
            status = 500;
            message = "Unable to connect to datasource.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResource(conn);
        }

        ResponseBean resp = new ResponseBean(status, message);
        resp.setId(teamID);
        return resp;
    }

    @PUT
    @Path("{teamid}/add")
    @Produces("application/json")
    public ResponseBean addPlayerToTeam(@PathParam("teamid") final int id, @QueryParam("login") final String playerLogin,
                                        @HeaderParam("token") final String token)
    {
        if (!PrivilegeUtil.hasTeamUpdate(token, id)) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";
        TeamBean team = new TeamBean();
        team.setId(id);
        if(team.getId() < 1)
        {
            status =  400;
            message = "Invalid team ID";
            return new ResponseBean(status, message);
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT INTO PlaysFor(Login, TeamID) Values (?, ?)");
            stmt.setString(1, playerLogin);
            stmt.setInt(2, team.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            status = 500;
            message = "Unable to add to team. SQL error.";
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

        return new ResponseBean(status, message);
    }

    @PUT
    @Path("{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateTeam(TeamBean team, @PathParam("id") final int id, @HeaderParam("token") final String token) {
        team.setId(id);
        return updateTeam(team, token);
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateTeam(TeamBean team, @HeaderParam("token") final String token) {
        String user = AuthenticationUtil.validateToken(token);
        if (!PrivilegeUtil.hasTeamUpdate(token, team.getId())) {
            return new ResponseBean(401, "Not authorized");
        }

        if (team.getOwner() == null) {
            team.setOwner(user);
        }

        int status = 200;
        String message;

        if (team.getId() < 1) {
            status = 400;
            message = "Invalid team ID.";
            return new ResponseBean(status, message);
        }

        if (!(message = team.validate()).isEmpty()) {
            status = 400;
            return new ResponseBean(status, message);
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();

            // first, check for existence of any teams or players
            if ((message = verifyTeamComponents(team, conn)) != null) {
                status = 422;
            }

            if (status == 200) {
                List<PreparedStatement> queries = createUpdateQueries(team, conn);
                for (PreparedStatement s : queries) {
                    // set stmt to s so we can close it in the catch if we hit an exception
                    stmt = s;
                    s.executeBatch();
                }
            }

        } catch (SQLException e) {
            status = 500;
            message = "Unable to update team. SQL Error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }

        return new ResponseBean(status, message);
    }

    @PUT
    @Path("{id}/colors")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateTeamColors(ColorBean color, @PathParam("id") final int teamID,
                                         @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasTeamUpdate(token, teamID)) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message;

        if (color.getId() < 1) {
            status = 400;
            message = "Invalid team ID.";
            return new ResponseBean(status, message);
        }

        if (!(message = color.validate()).isEmpty()) {
            status = 400;
            return new ResponseBean(status, message);
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("UPDATE TeamColors " +
                    "SET PrimaryColor = ?, SecondaryColor = ?, TertiaryColor = ? " +
                    "WHERE TeamId = ?");
            stmt.setString(1, color.getPrimaryColor());
            stmt.setString(2, color.getSecondaryColor());
            stmt.setString(3, color.getTertiaryColor());
            stmt.setInt(4, teamID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            status = 500;
            message = "Unable to update team colors. SQL Error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }

        return new ResponseBean(status, message);
    }

    @DELETE
    @Produces("application/json")
    public ResponseBean removePlayerFromTeam(@QueryParam("login") final String login, @QueryParam("teamId") final int teamId,
                                             @HeaderParam("token") final String token)
    {
        if (!PrivilegeUtil.hasTeamUpdate(token, teamId)) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";

        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = ConnectionManager.getInstance().getConnection();
            stmt = conn.prepareStatement("DELETE  FROM PlaysFor WHERE Login = ? AND TeamID = ?");
            stmt.setString(1, login);
            stmt.setInt(2, teamId);
            int res = stmt.executeUpdate();
            if(res < 1)
            {
                status = 404;
                message = "Player " + login + " does not belong to Team " + teamId + ".";
            }

        }
        catch (SQLException e)
        {
            status = 500;
            message =  "Unable to delete Team from League. SQL Error";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }
        return new ResponseBean(status, message);
    }

    @DELETE
    @Path("{id}")
    @Produces("application/json")
    public ResponseBean deleteTeam(@PathParam("id") final int id, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasTeamUpdate(token, id)) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("DELETE FROM Team WHERE TeamId = ?");
            stmt.setInt(1, id);
            int res = stmt.executeUpdate();
            if (res < 1) {
                status = 404;
                message = "Team with ID " + id + " does not exist.";
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to delete team. SQL Error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return new ResponseBean(status, message);
    }

    private static String verifyTeamComponents(TeamBean team, Connection conn) throws SQLException {
        String message = null;
        if (team.getOwner() != null) {
            if (!verifyTeamOwner(team.getOwner(), conn)) {
                message = "Non-existent Player specified for Team Owner.";
            }
        }
        return message;
    }

    /**
     * Make sure the Team Owner exists in the database
     * @param teamOwner
     * @param conn
     * @return true if the team exists
     * @throws SQLException
     */
    private static boolean verifyTeamOwner(String teamOwner, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(Login) FROM Player WHERE Login = ?");
        stmt.setString(1, teamOwner);
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
     * Add a team to the Team table
     * @param team
     * @param conn
     * @return the auto-generated event ID
     * @throws SQLException
     */
    private static int addTeam(TeamBean team, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO Team (TeamName, TeamOwner, Sport) " +
                "VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, team.getName());
        stmt.setString(2, team.getOwner());
        stmt.setString(3, team.getSport() != null ? team.getSport().name().toLowerCase() : null);
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

    /**
     * Create the update queries based on a team bean
     * @param team
     * @return set of queries mapped to whether or not the query is a batch
     */
    private List<PreparedStatement> createUpdateQueries(TeamBean team, Connection conn) throws SQLException {
        List<PreparedStatement> stmts = new LinkedList<PreparedStatement>();

        // update team stmt
        PreparedStatement stmt = conn.prepareStatement("UPDATE Team " +
                "SET TeamName = ?, TeamOwner = ?, Sport = ? " +
                "WHERE TeamId = ?");
        stmt.setString(1, team.getName());
        stmt.setString(2, team.getOwner());
        stmt.setString(3, team.getSport() != null ? team.getSport().name().toLowerCase() : null);
        stmt.setInt(4, team.getId());
        stmt.addBatch();
        stmts.add(stmt);
        return stmts;
    }
}
