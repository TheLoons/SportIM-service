package org.sportim.service;

import org.sportim.service.beans.AuthenticationBean;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.UserBean;
import org.sportim.service.util.AuthenticationUtil;

import javax.ws.rs.*;

/**
 * API for authenticating users
 */
@Path("/login")
public class LoginAPI {

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public ResponseBean authenticateUser(AuthenticationBean auth) {
        try {
            if (!AuthenticationUtil.authenticate(auth.getLogin(), auth.getPassword())) {
                return new ResponseBean(401, "Bad username or password");
            }
        } catch (Exception e) {
            // TODO log4j
            e.printStackTrace();
            return new ResponseBean(500, "Unable to generate auth token");
        }
        String token = AuthenticationUtil.generateToken(auth.getLogin());
        if (token == null) {
            return new ResponseBean(500, "Unable to generate auth token");
        }
        ResponseBean resp = new ResponseBean(200, "");
        resp.setToken(token);
        return resp;
    }

    @POST
    @Path("/validate")
    @Produces("application/json")
    @Consumes("application/json")
    public ResponseBean validateToken(@HeaderParam("token") String token, UserBean user) {
        String tokenUser = AuthenticationUtil.validateToken(token);
        if (tokenUser == null || !tokenUser.equals(user.getLogin())) {
            return new ResponseBean(401, "Invalid token");
        }
        return new ResponseBean(200, "Valid token");
    }
}
