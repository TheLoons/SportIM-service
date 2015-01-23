package org.sportim.service;

import org.sportim.service.beans.LeagueBean;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.StatusBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.ConnectionManager;
import org.sportim.service.util.ConnectionProvider;

import javax.ws.rs.*;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

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
    @Path("{id}")
    @Produces("application/json")
    public ResponseBean getLeague(@PathParam("id") final int leagueId)
    {
        int status = 200;
        String message = "";
        LeagueBean league = null;
        // TODO only return authorized leagues via auth token
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT LeagueId, LeagueName, LeagueOwner, Sport FROM League " +
                    "WHERE LeagueId = ?");
            stmt.setInt(1, leagueId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                league = new LeagueBean(rs);
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
        } else {
            StatusBean s = new StatusBean(404, "League not found.");
            resp.setStatus(s);
        }
        return resp;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean createLeague(LeagueBean league) {
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
    @Path("{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateLeague(LeagueBean league, @PathParam("id") final int id) {
        league.setLeagueId(id);
        return updateLeague(league);
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateLeague(LeagueBean league) {
        int status = 200;
        String message = "";

        if (league.getLeagueId() < 1) {
            status = 400;
            message = "Invalid league ID.";
            return new ResponseBean(status, message);
        }

        if (!(message = league.validate()).isEmpty()) {
            status = 400;
            return new ResponseBean(status, message);
        }

        // TODO AUTHENTICATE
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
    public ResponseBean deleteLeague(@PathParam("id") final int id) {
        int status = 200;
        String message = "";

        // TODO AUTHENTICATE
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
        stmt.setString(1, league.getLeagueName());
        stmt.setString(2, league.getLeagueOwner());
        stmt.setString(3, league.getSport());
        stmt.setInt(4, league.getLeagueId());
        stmt.addBatch();
        stmts.add(stmt);
        return stmts;
    }

    private static String verifyLeagueComponents(LeagueBean league, Connection conn) throws SQLException {

        String message = null;
        if (league.getLeagueId() > 0) {
            if (!verifyLeague(league.getLeagueId(), conn)) {
                message = "Non-existent league ID specified.";
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
     * Add a league to the league table
     * @param league league to add
     * @param conn connection to use
     * @return the auto-generated league ID
     * @throws java.sql.SQLException
     */
    private static int addLeague(LeagueBean league, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO League (LeagueName, LeagueOwner, Sport) " +
                "VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, league.getLeagueName());
        stmt.setString(2, league.getLeagueOwner());
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

