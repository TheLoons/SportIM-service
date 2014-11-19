package org.sportim.service;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Created by hannah on 11/18/14.
 */
@Path("/user")
@WebServlet
public class UserAPI {

    @GET
    @Produces("text/plain")
    public String congrats() {
        return "Congrats! You've made it to the user API!";
    }
}
