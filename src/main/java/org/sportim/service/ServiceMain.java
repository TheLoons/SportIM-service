package org.sportim.service;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;

/**
 * Created by hannah on 11/18/14.
 */
// The Java class will be hosted at the URI path "/helloworld"
@Path("/")
@WebServlet
public class ServiceMain {

    @GET
    @Produces("text/plain")
    public String getClichedMessage() {
        // TODO put API call directory here
        return "SportIM - api";
    }
}
