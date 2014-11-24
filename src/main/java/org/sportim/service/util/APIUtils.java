package org.sportim.service.util;

import org.json.JSONObject;

/**
 * Utility functions for the SportIM APIs.
 *
 * Created by hannah on 11/24/14.
 */
public class APIUtils {

    /**
     * Create a JSONObject containing an http status message, and
     * append it to a response.
     * @param response the JSONObject to append to
     * @param code the status code
     * @param message the reason for the status
     * @return the JSONObject response
     */
    public static JSONObject appendStatus(JSONObject response, int code, String message) {
        JSONObject status = new JSONObject();
        status.put("code", code);
        status.put("message", message);
        response.put("status", status);
        return response;
    }
}
