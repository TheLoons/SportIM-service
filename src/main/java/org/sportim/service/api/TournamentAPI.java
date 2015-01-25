package org.sportim.service.api;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.StatusBean;
import org.sportim.service.beans.TournamentBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

@Path("/tournament")
/**
 * Created by Doug on 12/4/14.
 */
public class TournamentAPI {
    private ConnectionProvider provider;

    public TournamentAPI() {
        provider = ConnectionManager.getInstance();
    }

    public TournamentAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

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
        // TODO only return authorized tournaments via auth token
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
        if (tournament != null) {
            resp.setTournament(tournament);
        } else {
            StatusBean s = new StatusBean(404, "Tournament not found.");
            resp.setStatus(s);
        }
        return resp;
    }

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
        PreparedStatement stmt = null;
        ResultSet rs = null;
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
        resp.setId(tournamentID);
        return resp;
    }

    @PUT
    @Path("{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateTournament(TournamentBean tournament, @PathParam("id") final int id,
                                         @HeaderParam("token") final String token) {
        tournament.setTournamentID(id);
        return updateTournament(tournament, token);
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean updateTournament(TournamentBean tournament, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasTournamentUpdate(token, tournament.getTournamentID())) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";

        if (tournament.getTournamentID() < 1) {
            status = 400;
            message = "Invalid tournament ID.";
            return new ResponseBean(status, message);
        }

        if (!(message = tournament.validate()).isEmpty()) {
            status = 400;
            return new ResponseBean(status, message);
        }

        // TODO AUTHENTICATE
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
            // TODO log
            e.printStackTrace();
            status = 500;
            message = "Unable to update tournament. SQL Error.";
        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }

        return new ResponseBean(status, message);
    }

    @DELETE
    @Path("{id}")
    @Produces("application/json")
    public ResponseBean deleteTournament(@PathParam("id") final int id, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasTournamentUpdate(token, id)) {
            return new ResponseBean(401, "Not authorized");
        }

        int status = 200;
        String message = "";

        // TODO AUTHENTICATE
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
            // TODO log actual error
            e.printStackTrace();
            status = 500;
            message = "Unable to delete tournament. SQL Error.";
        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
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
        stmt.setInt(4, tournament.getTournamentID());
        stmt.addBatch();
        stmts.add(stmt);
        return stmts;
    }

    private static String verifyTournamentComponents(TournamentBean tournament, Connection conn) throws SQLException {

        String message = null;
        int status = 200;
        if (status == 200 && tournament.getLeagueId() > 0) {
            if (!verifyLeague(tournament.getLeagueId(), conn)) {
                status = 422;
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
