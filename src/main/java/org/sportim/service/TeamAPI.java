package org.sportim.service;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.TeamBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.ConnectionManager;

import javax.ws.rs.*;

import java.sql.*;


@Path("/team")

/**
 * Created by Doug on 12/7/14.
 */
public class TeamAPI
{

    @GET
    @Path("{id}")
    @Produces("application/json")
    public ResponseBean getTeam(@QueryParam(value = "id") final int teamId)
    {
        int status = 200;
        String message = "";
        TeamBean team = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnectionManager.getInstance().getConnection();
            stmt = conn.prepareStatement("SELECT t1.TeamId, t1.TeamName, t1.TeamOwner FROM Team t1" +
                    "WHERE TeamId = " + teamId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                team = new TeamBean(rs);
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve tournament. SQL error.";
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
            resp.setTeam(team);
        }
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
    public static ResponseBean createDBTeam(TeamBean team) {
        int status = 200;
        String message = team.validate();
        if (!message.isEmpty()) {
            return new ResponseBean(400, message);
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnectionManager.getInstance().getConnection();

            // first, check for existence of any teams or players
            if ((message = verifyTeamComponents(team, conn)) != null) {
                status = 422;
            }

            // now, create the event and add any lookups
            conn.setAutoCommit(false);
            int teamID = -1;
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

        return new ResponseBean(status, message);
    }

    private static String verifyTeamComponents(TeamBean team, Connection conn) throws SQLException {

        String message = null;
        int status = 200;
        if (status == 200 && team.getOwner() != null) {
            if (!verifyTeamOwner(team.getOwner(), conn)) {
                status = 422;
                message = "Non-existent Player specified for Team Owner.";
            }
        }
        return message;
    }

    /**
     * Make sure the Team Owner exists in the database
     * @param teamOwner
     * @param conn
     * @return true if the tournament exists
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
}
