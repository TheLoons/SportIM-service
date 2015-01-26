package org.sportim.service.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;

import java.sql.Connection;

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

    /**
     * Try to close a resource
     * @param resource the resource to close
     * @return Returns true if closing was successful
     */
    public static boolean closeResource(AutoCloseable resource) {
        if (resource == null) {
            return true;
        }

        try {
            resource.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Try to close the given resources
     * @param resources resources to close
     */
    public static void closeResources(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            closeResource(resource);
        }
    }

    public static boolean setAutoCommit(Connection conn, boolean autoCommit) {
        if (conn == null) {
            return true;
        }

        try {
            conn.setAutoCommit(autoCommit);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Parse a date/time using the ISO date format
     * @param timestamp the datetime to parse
     * @return Returns a Joda DateTime
     */
    public static DateTime parseDateTime(String timestamp) {
        return ISODateTimeFormat.dateTimeParser().parseDateTime(timestamp);
    }

    /**
     * Turn millis since epoch into a UTC string
     * @param millis millis since epoch
     * @return UTC formatted string
     */
    public static String millisToUTCString(long millis) {
        DateTime dt = new DateTime(millis, DateTimeZone.UTC);
        return dt.toString();
    }
}
