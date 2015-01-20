package org.sportim.service;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.StatusBean;
import org.sportim.service.beans.TeamBean;
import org.sportim.service.beans.UserBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.ConnectionManager;
import org.sportim.service.util.ConnectionProvider;

import javax.ws.rs.*;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;


@Path("/team")

/**
 * Created by Doug on 12/7/14.
 */
public class TeamAPI {
    private ConnectionProvider provider;

    public TeamAPI() {
        provider = ConnectionManager.getInstance();
    }

    public TeamAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @GET
    @Path("{id}")
    @Produces("application/json")
    public ResponseBean getTeam(@PathParam("id") final int teamId)
    {
        int status = 200;
        List<UserBean> players = new LinkedList<UserBean>();
        String message = "";
        TeamBean team = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT t1.TeamId, t1.TeamName, t1.TeamOwner FROM Team t1 " +
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

            stmt = conn.prepareStatement("SELECT p1.Login, p1.FirstName, p1.LastName from Player p1, PlaysFor pf WHERE pf.TeamID = ?");
            stmt.setInt(1, teamId);
            rs = stmt.executeQuery();

            while(rs.next())
            {
                players.add(new UserBean(rs));
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve team. SQL error.";
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
    @Produces("application/json")
    public ResponseBean getTeams(@QueryParam(value="league") final int leagueID)
    {
        int status = 200;
        String message = "";
        List<TeamBean> teams = new LinkedList<TeamBean>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT t1.TeamId, t1.TeamName, t1.TeamOwner FROM Team t1, TeamBelongsTo l " +
                    "WHERE l.LeagueId = ? AND t1.TeamId = l.TeamId");
            stmt.setInt(1, leagueID);
            rs = stmt.executeQuery();

            while (rs.next()) {
                teams.add(new TeamBean(rs));
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve teams. SQL error.";
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
        resp.setTeams(teams);
        return resp;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean createTeam(TeamBean team) {
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
        PreparedStatement stmt = null;
        ResultSet rs = null;
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
        resp.setId(teamID);
        return resp;
    }

//    @POST
//    @Consumes("application/json")
//    @Produces("application/json")
//    public ResponseBean addPlayerToTeam(TeamBean team, @QueryParam("id") final int id, @QueryParam("login") final String login)
//    {
//        team.setId(id);
//        return addPlayerToTeam(team, login);
//    }
//
//    @POST
//    @Consumes("application/json")
//    @Produces("application/json")
//    public ResponseBean addPlayerToTeam(TeamBean team, String playerLogin)
//    {
//        int status = 200;
//        String message = "";
//
//        if(team.getId() < 1)
//        {
//            status =  400;
//            message = "Invalid team ID";
//            return new ResponseBean(status, message);
//        }
//
//        Connection conn = null;
//        PreparedStatement stmt = null;
//        ResultSet rs = null;
//        try {
//            conn = provider.getConnection();
//
//
//            // now, create the event and add any lookups
//            conn.setAutoCommit(false);
//            if (status == 200)
//            {
//                UserBean player = new UserBean();
//                player.setLogin(playerLogin);
//                if(!verifyPlayer(player, conn).isEmpty())
//                {
//                    message = "Player Not Found";
//                    status = 404;
//                }
//            }
//            if(status == 200)
//            {
//                stmt = conn.prepareStatement("INSERT INTO PlaysFor(Login, TeamID) Values (?, ?)");
//                stmt.setString(1, playerLogin);
//                stmt.setInt(2, team.getId());
//                rs = stmt.executeQuery();
//                if(!rs.next())
//                {
//                    status = 500;
//                    message = "Unable to link player to team.";
//                }
//            }
//            if (status == 200) {
//                conn.commit();
//            }
//        } catch (SQLException e) {
//            status = 500;
//            message = "Unable to add team. SQL error.";
//            // TODO log4j 2 log this
//            e.printStackTrace();
//        } catch (NullPointerException e) {
//            status = 500;
//            message = "Unable to connect to datasource.";
//            // TODO log4j 2 log this
//            e.printStackTrace();
//        } finally {
//            boolean ok = APIUtils.closeResource(rs);
//            ok = ok && APIUtils.closeResource(stmt);
//            ok = ok && APIUtils.closeResource(conn);
//            if (!ok) {
//                // TODO implement Log4j 2 and log out error
//            }
//        }
//
//
//        ResponseBean resp = new ResponseBean(status, message);
//        return resp;
//    }

    @PUT
    @Path("{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateTeam(TeamBean team, @PathParam("id") final int id) {
        team.setId(id);
        return updateTeam(team);
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateTeam(TeamBean team) {
        int status = 200;
        String message = "";

        if (team.getId() < 1) {
            status = 400;
            message = "Invalid team ID.";
            return new ResponseBean(status, message);
        }

        if (!(message = team.validate()).isEmpty()) {
            status = 400;
            return new ResponseBean(status, message);
        }

        // TODO AUTHENTICATE
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
            // TODO log
            e.printStackTrace();
            status = 500;
            message = "Unable to update team. SQL Error.";
        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }

        return new ResponseBean(status, message);
    }

    @DELETE
    @Path("{id}")
    @Produces("application/json")
    public ResponseBean deleteTeam(@PathParam("id") final int id) {
        int status = 200;
        String message = "";

        // TODO AUTHENTICATE
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
            // TODO log actual error
            e.printStackTrace();
            status = 500;
            message = "Unable to delete team. SQL Error.";
        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return new ResponseBean(status, message);
    }

    private static String verifyPlayer(UserBean player, Connection conn) throws SQLException
    {
        String message = null;


        if(player.getLogin() != null && !player.getLogin().isEmpty())
        {
            PreparedStatement stmt = conn.prepareStatement("SELECT  COUNT (Login) FROM Player Where Login = ?");
            stmt.setString(1, player.getLogin());
            ResultSet rs = stmt.executeQuery();
            if(rs.next() && rs.getInt(1) != 1)
            {
                message = "Player not available";
            }
        }
        return message;
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
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO Team (TeamName, TeamOwner) " +
                "VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, team.getName());
        stmt.setString(2, team.getOwner());
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
                "SET TeamName = ?, TeamOwner = ?" +
                "WHERE TeamId = ?");
        stmt.setString(1, team.getName());
        stmt.setString(2, team.getOwner());
        stmt.setInt(3, team.getId());
        stmt.addBatch();
        stmts.add(stmt);
        return stmts;
    }
}
