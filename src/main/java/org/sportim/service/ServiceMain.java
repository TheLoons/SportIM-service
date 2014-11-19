package org.sportim.service;

import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;

/**
 * Servlet displays call directory for the SportIM API.
 *
 * @author hbrock
 */
@Path("/")
@WebServlet
public class ServiceMain {
    public static final String VERSION = "0.1";

    @GET
    @Produces("application/json")
    public String getDirectory() {
        JSONObject json = new JSONObject();
        json.put("version", VERSION);

        JSONObject directory = new JSONObject();
        directory.put("users", "/sportim/rest/user");

        json.put("directory", directory);

        return json.toString();
    }
}
