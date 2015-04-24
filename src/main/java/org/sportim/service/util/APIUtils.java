package org.sportim.service.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;

/**
 * Utility functions for the SportIM APIs.
 *
 * @Author Hannah Brock
 */
public class APIUtils {

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

    /**
     * Set auto commit on a connection
     * @param conn the connection
     * @param autoCommit whether to autocommit or not
     * @return true if successful, false otherwise
     */
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

    /**
     * Create a parameter string for a SQL query
     * @param numParams the number of parameters
     * @return A string of ?s, comma separated
     */
    public static String createParamString(int numParams) {
        String params = "";
        for (int i = 0; i < numParams; i++) {
            if (!params.isEmpty()) {
                params += ",";
            }
            params += "?";
        }
        return params;
    }

    /**
     * Get a stack trace as a plain ol' string
     * @param e the exception with the trace
     * @return The exception's stacktrace as a String
     */
    public static String getStacktraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
