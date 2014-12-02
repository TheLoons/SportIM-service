package org.sportim.service.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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
     * Get a UTC date from a result set
     * @param rs the result set
     * @param col the column containing the date
     * @return Returns the date as a Joda DateTime
     * @throws SQLException
     */
    public static DateTime getUTCDateFromResultSet(ResultSet rs, int col) throws SQLException {
        Long millis = rs.getLong(col);
        return new DateTime(millis, DateTimeZone.UTC);
    }

    /**
     * Get a new random salt
     * @return the byte array containing the salt
     */
    public static byte[] getSalt() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[20];
        random.nextBytes(bytes);
        return bytes;
    }

    /**
     * Get a salted and hashed password
     * @param salt the salt to use
     * @param pwd the password
     * @return Null if unsuccessful, byte array of the hash otherwise.
     */
    public static byte[] saltHashPassword(byte[] salt, String pwd) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(salt);
            outputStream.write(pwd.getBytes("UTF-8"));
        } catch (IOException e) {
            // TODO log4j this
            e.printStackTrace();
            return null;
        }

        byte[] bytes = outputStream.toByteArray();
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // TODO log4j this
            e.printStackTrace();
            return null;
        }
        byte[] hash = digest.digest(bytes);

        return hash;
    }

    /**
     * Convert a byte array into a hex string.
     * @param bytes
     * @return
     */
    public static String byteArrayToHexString(byte[] bytes) {
        String hex = DatatypeConverter.printHexBinary(bytes);
        return hex;
    }
}
