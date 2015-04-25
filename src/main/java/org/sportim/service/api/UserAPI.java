package org.sportim.service.api;

import org.apache.log4j.Logger;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.StatusBean;
import org.sportim.service.beans.TeamBean;
import org.sportim.service.beans.UserBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * API to handle user requests.
 */
@Path("/user")
public class UserAPI {
    private static Logger logger = Logger.getLogger(UserAPI.class.getName());
    private ConnectionProvider provider;

    public UserAPI() {
        provider = ConnectionManager.getInstance();
    }

    public UserAPI(ConnectionProvider provider) {
        this.provider = provider;
    }
    /* Get request to grab a View version of a user */
    @GET
    @Path("view")
    @Produces("application/json")
    public ResponseBean getUsersForView(@HeaderParam("token") final String token) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return new ResponseBean(401, "Not authorized");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int status = 200;
        List<UserBean> users = new LinkedList<UserBean>();
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT DISTINCT p.Login, p.FirstName, p.LastName FROM Player p " +
                    "LEFT OUTER JOIN PlaysFor pf ON p.Login = pf.Login LEFT OUTER JOIN Team t ON t.TeamId = pf.TeamId " +
                    "LEFT OUTER JOIN TeamBelongsTo tb ON tb.TeamId = t.TeamId " +
                    "LEFT OUTER JOIN League l ON tb.LeagueId = l.LeagueId " +
                    "WHERE l.LeagueOwner = ? OR t.TeamOwner = ? OR p.Login = ?");
            stmt.setString(1, user);
            stmt.setString(2, user);
            stmt.setString(3, user);
            rs = stmt.executeQuery();
            while (rs.next()) {
                UserBean userb = new UserBean();
                userb.setLogin(rs.getString(1));
                userb.setFirstName(rs.getString(2));
                userb.setLastName(rs.getString(3));
                users.add(userb);
            }
        } catch (Exception e) {
            logger.error("Unable to get users for viewing: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            status = 500;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (status != 200) {
            return new ResponseBean(status, "Unable to retrieve users.");
        }
        ResponseBean resp = new ResponseBean(status, "");
        resp.setUsers(users);
        return resp;
    }
    /* Driver method to get the user page for individual display */
    @GET
    @Path("{login}")
    @Produces("application/json")
    public ResponseBean getUser(@PathParam("login") final String login, @HeaderParam("token") final String token) {
        return getUserQuery(login, token);
    }
    /* Main method for getting user information
     * @QueryParam login - Login of the player you are trying to grab
     * @HeaderParam token - Token needed to determine if user is eligible to view the player
     */
    @GET
    @Produces("application/json")
    public ResponseBean getUserQuery(@QueryParam("login") final String login,
                                     @HeaderParam("token") final String token) {
        int status = 200;
        String message = "";
        // Check if login is displayed
        if (login == null) {
            return new ResponseBean(400, "Missing login parameter");
        }
        // See if user is authorized to access the player
        if (!PrivilegeUtil.hasUserView(token, login)) {
            return new ResponseBean(401, "Not authorized");
        }
        // Build Connection variables
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        UserBean user = null;
        try {
            // Get connection from connection manager
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT FirstName, LastName, Phone FROM Player " +
                                         "WHERE Login = ?");
            stmt.setString(1, login);
            rs = stmt.executeQuery();
            // Get result if found
            if (rs.next()) {
                user = new UserBean(rs, login);
            }
            // Indicate user not found if not found
            else {
                status = 404;
                message = "User " + login + " not found.";
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve events. SQL error.";
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
        // Build response to send back
        ResponseBean resp = new ResponseBean(status, message);
        if (user != null) {
            resp.setUser(user);
        } else {
            StatusBean s = new StatusBean(404, "User not found.");
            resp.setStatus(s);
        }
        return resp;
    }

    /* Get user with all information, used for Setting page
      @QueryParam login - Login of the player your are trying to get
      @HeaderParam token - Authorization token
     */
    @GET
    @Path("/alert")
    @Produces("application/json")
    public ResponseBean getUserQueryWithAlert(@QueryParam("login") final String qlogin,
                                              @HeaderParam("token") final String token) {
        int status = 200;
        long millisPerHour = 3600000;
        String message = "";

        String login = qlogin;
        // If login is null, go grab login based off of token
        if (login == null) {
            login = AuthenticationUtil.validateToken(token);
        }
        // If login is still null, notify caller that login is missing
        if (login == null) {
            return new ResponseBean(400, "Missing login parameter");
        }
        // Check if token is allowed to edit user
        if (!PrivilegeUtil.hasUserView(token, login)) {
            return new ResponseBean(401, "Not authorized");
        }
        // Build Connection variables
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        UserBean user = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT FirstName, LastName, Phone, GameAlert, PracticeAlert, MeetingAlert, OtherAlert, ReceiveEmail, ReceiveText FROM Player " +
                    "WHERE Login = ?");
            stmt.setString(1, login);
            rs = stmt.executeQuery();
            // Build User bean for response
            if (rs.next()) {
                user = new UserBean(rs, login);
                user.setGameAlert(rs.getLong(4) / millisPerHour);
                user.setPracticeAlert(rs.getLong(5) / millisPerHour);
                user.setMeetingAlert(rs.getLong(6) / millisPerHour);
                user.setOtherAlert(rs.getLong(7) / millisPerHour);
                user.setReceiveEmail(rs.getInt(8));
                user.setReceiveText(rs.getInt(9));
            }
            // User not found
            else {
                status = 404;
                message = "User " + login + " not found.";
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve events. SQL error.";
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
        // Build Response Bean
        ResponseBean resp = new ResponseBean(status, message);
        if (user != null) {
            resp.setUser(user);
        } else {
            StatusBean s = new StatusBean(404, "User not found.");
            resp.setStatus(s);
        }
        return resp;
    }

    /*
    Post call to create user
    @Param user - User bean with needed information to create user
     */
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public ResponseBean createUser(UserBean user) {
        int status = 200;
        String message = user.validate(true);
        if (!message.isEmpty()) {
            return new ResponseBean(400, message);
        }

        message = "";
        // Get Salt for password hashing
        byte[] salt = AuthenticationUtil.getSalt();
        // Salt hash password, so password isn't in plain text
        byte[] hash = AuthenticationUtil.saltHashPassword(salt, user.getPassword());

        // Build Connection and submit user
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("INSERT IGNORE INTO Player (Login, FirstName, LastName, Phone, Password, Salt) " +
                                         "VALUES (?,?,?,?,?,?)");
            stmt.setString(1, user.getLogin());
            stmt.setString(2, user.getFirstName());
            stmt.setString(3, user.getLastName());
            stmt.setString(4, user.getPhone());
            stmt.setString(5, AuthenticationUtil.byteArrayToHexString(hash));
            stmt.setString(6, AuthenticationUtil.byteArrayToHexString(salt));
            int res = stmt.executeUpdate();
            if (res < 1) {
                message = "User with that login already exists.";
                status = 400;
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to add user. SQL error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }
        // Return response
        return new ResponseBean(status, message);
    }

    @PUT
    @Path("/alert")
    @Produces("application/json")
    @Consumes("application/json")
    public ResponseBean updateUserAlerts(UserBean user, @HeaderParam("token") final String token) {
        long millisPerHour = 3600000;
        int status = 200;
        String message = "";
        // Validate Login
        if (user.getLogin() == null) {
            user.setLogin(AuthenticationUtil.validateToken(token));
        }

        if (!(message = user.validate(false)).isEmpty()) {
            status = 400;
            return new ResponseBean(status, message);
        }
        // Authenticate user
        if (!PrivilegeUtil.hasUserUpdate(token, user.getLogin())) {
            return new ResponseBean(401, "Not authorized");
        }

        // Build connection
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            // If player is already in there with hashed password, only insert relevant elements
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                stmt = conn.prepareStatement("UPDATE Player SET FirstName = ?, LastName = ?, Phone = ?, GameAlert = ?, PracticeAlert = ?, MeetingAlert = ?, OtherAlert = ?, ReceiveEmail = ?, ReceiveText = ? WHERE Login = ?");
                stmt.setString(10, user.getLogin());
            }
            // Otherwise, hash/salt password and insert
            else {
                stmt = conn.prepareStatement("UPDATE Player SET FirstName = ?, LastName = ?, Phone = ?, GameAlert = ?, PracticeAlert = ?, MeetingAlert = ?, OtherAlert = ?, ReceiveEmail = ?, ReceiveText = ?, Password = ?, Salt = ? " +
                        "WHERE Login = ?");
                byte[] salt = AuthenticationUtil.getSalt();
                byte[] hash = AuthenticationUtil.saltHashPassword(salt, user.getPassword());
                stmt.setString(10, AuthenticationUtil.byteArrayToHexString(hash));
                stmt.setString(11, AuthenticationUtil.byteArrayToHexString(salt));
                stmt.setString(12, user.getLogin());
            }
            stmt.setString(1, user.getFirstName());
            stmt.setString(2, user.getLastName());
            stmt.setString(3, user.getPhone());
            stmt.setLong(4, user.getGameAlert() * millisPerHour);
            stmt.setLong(5, user.getPracticeAlert() * millisPerHour);
            stmt.setLong(6, user.getMeetingAlert() * millisPerHour);
            stmt.setLong(7, user.getOtherAlert() * millisPerHour);
            stmt.setInt(8, user.getReceiveEmail());
            stmt.setInt(9, user.getReceiveText());
            String statement = stmt.toString();
            int res = stmt.executeUpdate();
            if (res < 1) {
                message = "No change to user.";
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to update user. SQL error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }
        return new ResponseBean(status, message);

    }
    // Put command for updating user without alerts
    @PUT
    @Path("{login}")
    @Produces("application/json")
    @Consumes("application/json")
    public ResponseBean updateUser(@PathParam("login") final String login, UserBean user,
                                   @HeaderParam("token") final String token) {
        user.setLogin(login);
        return updateUser(user, token);
    }

    // Put command to update user without alert information
    @PUT
    @Produces("application/json")
    @Consumes("application/json")
    public ResponseBean updateUser(UserBean user, @HeaderParam("token") final String token) {
        int status = 200;
        String message = "";

        if (!(message = user.validate(false)).isEmpty()) {
            status = 400;
            return new ResponseBean(status, message);
        }

        if (!PrivilegeUtil.hasUserUpdate(token, user.getLogin())) {
            return new ResponseBean(401, "Not authorized");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                stmt = conn.prepareStatement("UPDATE Player SET FirstName = ?, LastName = ?, Phone = ? " +
                                             "WHERE Login = ?");
                stmt.setString(4, user.getLogin());
            } else {
                stmt = conn.prepareStatement("UPDATE Player SET FirstName = ?, LastName = ?, Phone = ?, Password = ?, Salt = ? " +
                                             "WHERE Login = ?");
                byte[] salt = AuthenticationUtil.getSalt();
                byte[] hash = AuthenticationUtil.saltHashPassword(salt, user.getPassword());
                stmt.setString(4, AuthenticationUtil.byteArrayToHexString(hash));
                stmt.setString(5, AuthenticationUtil.byteArrayToHexString(salt));
                stmt.setString(6, user.getLogin());
            }
            stmt.setString(1, user.getFirstName());
            stmt.setString(2, user.getLastName());
            stmt.setString(3, user.getPhone());
            int res = stmt.executeUpdate();
            if (res < 1) {
                message = "No change to user.";
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to update user. SQL error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }
        return new ResponseBean(status, message);
    }
    /*
        Delete user from system
        @PathParam login - Login of player to be deleted
        @HeaderParam token - Authentication Token
     */
    @DELETE
    @Path("{login}")
    @Produces("application/json")
    public ResponseBean deleteUser(@PathParam("login") final String login, @HeaderParam("token") final String token) {
        String message = "";
        int status = 200;
        // Check if user is allowed to delete the player
        if (!PrivilegeUtil.hasUserUpdate(token, login)) {
            return new ResponseBean(401, "Not authorized");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("DELETE FROM Player WHERE Login = ?");
            stmt.setString(1, login);
            int res = stmt.executeUpdate();

            // If not found, update status and message for response
            if (res < 1) {
                status = 404;
                message = "User not found.";
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to delete user. SQL Error.";
            logger.error(message + ": " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(stmt, conn);
        }
        return new ResponseBean(status, message);
    }
}
