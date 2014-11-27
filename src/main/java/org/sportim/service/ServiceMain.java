package org.sportim.service;

import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import java.sql.SQLException;

/**
 * Servlet displays call directory for the SportIM API.
 *
 * @author hbrock
 */
@Path("/directory")
@WebServlet
public class ServiceMain {
    public static final String VERSION = "0.1";

    @GET
    @Produces("application/json")
    public String getDirectory() throws SQLException {
        JSONObject json = new JSONObject();
        json.put("version", VERSION);

        JSONObject directory = new JSONObject();
        directory.put("users", "/rest/user");
        directory.put("bulk events", "/rest/events");

        json.put("directory", directory);

        return json.toString();
    }
}
