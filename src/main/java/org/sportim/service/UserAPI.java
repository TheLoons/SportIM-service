package org.sportim.service;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.UserBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.ConnectionManager;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * API to handle user requests.
 *
 * Created by hannah on 11/18/14.
 */
@Path("/user")
public class UserAPI {

    @GET
    @Produces("application/json")
    public ResponseBean getUser(@QueryParam(value = "login") final String login,
                          @QueryParam(value = "token") final String token) {
        int status = 200;
        String message = "";

        if (login == null) {
            status = 400;
            message = "Missing login parameter";
            return new ResponseBean(status, message);
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        UserBean user = null;
        // TODO only pull user if authorized by token
        try {
            conn = ConnectionManager.getInstance().getConnection();
            stmt = conn.prepareStatement("SELECT FirstName, LastName, Phone FROM Player " +
                                         "WHERE Login = ?");
            stmt.setString(1, login);
            rs = stmt.executeQuery();

            if (rs.next()) {
                user = new UserBean(rs, login);
            }
        } catch (SQLException e) {
            status = 500;
            message = "Unable to retrieve events. SQL error.";
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
        if (user != null) {
            resp.setUser(user);
        }
        return resp;
    }

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public ResponseBean createUser(UserBean user) {
        int status = 200;
        String message = user.validate(true);
        if (message != null) {
            return new ResponseBean(400, message);
        }

        message = "";
        byte[] salt = APIUtils.getSalt();
        byte[] hash = APIUtils.saltHashPassword(salt, user.getPassword());

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnectionManager.getInstance().getConnection();
            stmt = conn.prepareStatement("INSERT IGNORE INTO Player (Login, FirstName, LastName, Phone, Password, Salt) " +
                                         "VALUES (?,?,?,?,?,?)");
            stmt.setString(1, user.getLogin());
            stmt.setString(2, user.getFirstName());
            stmt.setString(3, user.getLastName());
            stmt.setString(4, user.getPhone());
            stmt.setString(5, APIUtils.byteArrayToHexString(hash));
            stmt.setString(6, APIUtils.byteArrayToHexString(salt));
            int res = stmt.executeUpdate();
            if (res < 1) {
                message = "User with that login already exists.";
                status = 400;
            }
        } catch (SQLException e) {
            // TODO log4j this
            e.printStackTrace();
            status = 500;
            message = "Unable to add user. SQL error.";
        } finally {
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }

        return new ResponseBean(status, message);
    }

    @PUT
    @Produces("application/json")
    @Consumes("application/json")
    public ResponseBean updateUser(UserBean user) {
        return new ResponseBean(501, "Not implemented");
    }

    @DELETE
    @Produces("application/json")
    public ResponseBean deleteUser(@QueryParam(value = "login") final String login,
                                   @QueryParam(value = "token") final String token) {
        return new ResponseBean(501, "Not implemented");
    }
}
