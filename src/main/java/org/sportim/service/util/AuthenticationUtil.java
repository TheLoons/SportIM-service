package org.sportim.service.util;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.sportim.service.beans.UserBean;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Utililties for generating and using authentication tokens.
 *
 * @author Hannah Brock
 */
public class AuthenticationUtil {

    /**
     * Generate and store an auth token.
     * @param username User to generate auth token for
     * @return the token
     */
    public static String generateToken(String username) {
        String token = UUID.randomUUID().toString().toUpperCase() + "#" + username + "#" + System.nanoTime();
        StandardPBEStringEncryptor jasypt = new StandardPBEStringEncryptor();
        String enToken = jasypt.encrypt(token);
        storeToken(username, enToken);
        return enToken;
    }

    /**
     * Store a token in the database. This token will overwrite any existing token
     * for the given user.
     * @param username The user
     * @param token The auth token
     * @return true if successful
     */
    private static boolean storeToken(String username, String token) {
        Connection conn = null;
        PreparedStatement stmt = null;
        int upCount = 0;
        try {
            conn = ConnectionManager.getInstance().getConnection();
            stmt = conn.prepareStatement("INSERT INTO Auth (Login, Token) VALUES (?, ?) " +
                                         "ON DUPLICATE KEY UPDATE Token = ?");
            stmt.setString(1, username);
            stmt.setString(2, token);
            stmt.setString(3, token);
            upCount = stmt.executeUpdate();
        } catch (SQLException e) {
            // TODO: log4j this
            e.printStackTrace();
        } finally {
            APIUtils.closeResource(conn);
            APIUtils.closeResource(stmt);
        }
        return upCount > 0;
    }

    /**
     * Validates an existing token.
     * @param token the token
     * @return the user belonging to the token, or null if the token is invalid
     */
    public static String validateToken(String token) {
        StandardPBEStringEncryptor jasypt = new StandardPBEStringEncryptor();
        String[] parts = jasypt.decrypt(token).split("#");
        if (parts.length < 3) {
            return null;
        }
        // TODO: check DB for token

        // TODO: return user
        return null;
    }

    /**
     * Check if a user + password combination is valid.
     * @param username the username
     * @param password the password
     * @return true if valid
     */
    public static boolean authenticate(String username, String password) {
        UserBean user = getUserFromDB(username);
        byte[] salt = hexStringToByteArray(user.getSalt());
        byte[] currentPass = saltHashPassword(salt, password);
        byte[] correctPass = hexStringToByteArray(user.getPassword());
        for (int i = 0; i < Math.min(currentPass.length, correctPass.length); i++) {
            if (currentPass[i] != correctPass[i]) {
                return false;
            }
        }
        return true;
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
        return DatatypeConverter.printHexBinary(bytes);
    }

    public static byte[] hexStringToByteArray(String hex) {
        return DatatypeConverter.parseHexBinary(hex);
    }

    private static UserBean getUserFromDB(String username) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        UserBean user = null;
        try {
            conn = ConnectionManager.getInstance().getConnection();
            stmt = conn.prepareStatement("SELECT FirstName, LastName, Phone, Password, Salt FROM Player " +
                    "WHERE Login = ?");
            stmt.setString(1, username);
            rs = stmt.executeQuery();

            if (rs.next()) {
                user = new UserBean(rs, username);
                user.setPassword(rs.getString(4));
                user.setSalt(rs.getString(5));
            }
        } catch (SQLException e) {
            // TODO log4j 2 log this
            e.printStackTrace();
            return null;
        } catch (NullPointerException e) {
            // TODO log4j 2 log this
            e.printStackTrace();
            return null;
        } finally {
            APIUtils.closeResource(rs);
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return user;
    }
}