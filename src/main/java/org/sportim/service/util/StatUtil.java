package org.sportim.service.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Utilities for the soccer stat tracking API.
 */
public class StatUtil {
    private static ConnectionProvider provider = ConnectionManager.getInstance();

    public static void setConnectionProvider(ConnectionProvider provider) {
        StatUtil.provider = provider;
    }

    /**
     * Check if the given session is valid for this event
     * @param sessionID the session id
     * @param eventID the event id
     * @return true if the session is valid
     */
    public static boolean isValidSession(String sessionID, int eventID) {
        if (sessionID == null || sessionID.isEmpty() || eventID < 1) {
            return false;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean valid = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT COUNT(sessionID) FROM StatSessions " +
                    "WHERE eventID = ? and sessionID = ?");
            stmt.setInt(1, eventID);
            stmt.setString(2, sessionID);
            rs = stmt.executeQuery();
            valid = rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
        return valid;
    }
}
