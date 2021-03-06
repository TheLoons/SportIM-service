package org.sportim.service.api;

import org.apache.log4j.Logger;
import org.sportim.service.beans.AuthenticationBean;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.UserBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.AuthenticationUtil;

import javax.ws.rs.*;

/**
 * API for authenticating users
 */
@Path("/login")
public class LoginAPI {
    private static Logger logger = Logger.getLogger(LoginAPI.class.getName());

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public ResponseBean authenticateUser(AuthenticationBean auth) {
        try {
            if (!AuthenticationUtil.authenticate(auth.getLogin(), auth.getPassword())) {
                return new ResponseBean(401, "Bad username or password");
            }
        } catch (Exception e) {
            logger.error("Error authenticating:" + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
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
