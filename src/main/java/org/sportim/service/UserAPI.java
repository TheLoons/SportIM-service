package org.sportim.service;

import org.json.JSONObject;
import org.sportim.service.util.APIUtils;

import javax.ws.rs.*;

/**
 * API to handle user requests.
 *
 * Created by hannah on 11/18/14.
 */
@Path("/user")
public class UserAPI {

    @GET
    @Produces("application/json")
    public String getUser() {
        JSONObject response = new JSONObject();

        APIUtils.appendStatus(response, 501, "Not Implemented");
        return response.toString();
    }

    @POST
    @Produces("application/json")
    public String createUser(@QueryParam(value = "email") final String email,
                             @QueryParam(value = "first") final String first,
                             @QueryParam(value = "last") final String last,
                             @QueryParam(value = "phone") final String phone,
                             @QueryParam(value = "pwd") final String pwd) {
        JSONObject response = new JSONObject();
        if (!checkParams(response, email, first, last, phone, pwd)) {
            return response.toString();
        }


        APIUtils.appendStatus(response, 501, "Not Implemented");
        return response.toString();
    }

    /**
     * Check the parameters and append the appropriate response
     * @param response the response to send
     * @param email email from the http request
     * @param first first name from the http request
     * @param last last name from the http request
     * @param phone phone number from the http request
     * @param pwd password from the http request
     * @return true if all parameters are acceptable
     */
    private static boolean checkParams(JSONObject response, String email, String first, String last, String phone,
                                       String pwd) {
        if (email == null) {
            APIUtils.appendStatus(response, 400, "User email/login must be specified");
            return false;
        }

        if (first == null) {
            APIUtils.appendStatus(response, 400, "First name must be specified");
            return false;
        }

        if (last == null) {
            APIUtils.appendStatus(response, 400, "Last name must be specified");
            return false;
        }

        if (phone == null) {
            APIUtils.appendStatus(response, 400, "Phone number must be specified");
            return false;
        }
        // TODO verify phone number format

        if (pwd == null) {
            APIUtils.appendStatus(response, 400, "Password must be specified");
            return false;
        }

        return true;
    }

    @PUT
    @Produces("application/json")
    public String updateUser() {
        JSONObject response = new JSONObject();

        APIUtils.appendStatus(response, 501, "Not implemented");
        return response.toString();
    }

    @DELETE
    @Produces("application/json")
    public String deleteUser() {
        JSONObject response = new JSONObject();

        APIUtils.appendStatus(response, 501, "Not Implemented");
        return response.toString();
    }
}
