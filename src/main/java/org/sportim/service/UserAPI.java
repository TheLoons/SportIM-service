package org.sportim.service;

import org.json.JSONObject;
import org.sportim.service.beans.UserBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.ConnectionManager;

import javax.ws.rs.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    public String getUser(@QueryParam(value = "login") final String login,
                          @QueryParam(value = "token") final String token) {
        JSONObject response = new JSONObject();
        int status = 200;
        String message = "";

        if (login == null) {
            status = 400;
            message = "Missing login parameter";
        }
        else {
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            JSONObject user = new JSONObject();
            // TODO only pull user if authorized by token
            try {
                conn = ConnectionManager.getInstance().getConnection();
                stmt = conn.prepareStatement("SELECT FirstName, LastName, Phone FROM Player " +
                                             "WHERE Login = ?");
                stmt.setString(1, login);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    user.put("email", login);
                    user.put("firstName", rs.getString(1));
                    user.put("lastName", rs.getString(2));
                    user.put("phone", rs.getString(3));
                }
                response.put("user", user);
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
        }

        APIUtils.appendStatus(response, status, message);
        return response.toString();
    }

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public String createUser(UserBean user) {
        JSONObject response = new JSONObject();
        int status = 200;

        String message = user.validate(true);
        if (message == null) {
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
        }
        else {
            status = 400;
        }

        APIUtils.appendStatus(response, status, message);
        return response.toString();
    }

    @PUT
    @Produces("application/json")
    @Consumes("application/json")
    public String updateUser(UserBean user) {
        JSONObject response = new JSONObject();

        APIUtils.appendStatus(response, 501, "Not implemented");
        return response.toString();
    }

    @DELETE
    @Produces("application/json")
    public String deleteUser(@QueryParam(value = "login") final String login,
                             @QueryParam(value = "token") final String token) {
        JSONObject response = new JSONObject();

        APIUtils.appendStatus(response, 501, "Not Implemented");
        return response.toString();
    }
}
