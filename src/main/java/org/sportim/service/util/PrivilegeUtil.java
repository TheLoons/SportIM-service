package org.sportim.service.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Utilities for determining availability of actions to a user.
 */
public class PrivilegeUtil {
    private static ConnectionProvider provider = ConnectionManager.getInstance();

    public static void setConnectionProvider(ConnectionProvider provider) {
        PrivilegeUtil.provider = provider;
    }

    /**
     * A user must be the team/league owner of an organization to which the
     * requested user belongs or be the user to have user view privileges.
     * @param token the token of the user requesting the view
     * @param userToView the user being requested
     * @return true if the requesting user can view the given user
     */
    public static boolean hasUserView(String token, String userToView) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return false;
        }

        boolean ok = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT COUNT(p1.Login) FROM " +
                    "Player p1 INNER JOIN PlaysFor pf ON p1.Login = pf.Login " +
                    "INNER JOIN Team t ON t.TeamId = pf.TeamID " +
                    "LEFT OUTER JOIN League l ON t.TeamId = l.LeagueId " +
                    "WHERE p1.Login = ? " +
                    "AND (t.TeamOwner = ? " +
                    "     OR l.LeagueOwner = ?)");
            stmt.setString(1, userToView);
            stmt.setString(2, user);
            stmt.setString(3, user);
            rs = stmt.executeQuery();
            if (rs.next()) {
                ok = rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            ok = false;
            // TODO log4j
            e.printStackTrace();
        } finally {
            APIUtils.closeResource(rs);
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return ok;
    }

    public static boolean hasUserUpdate(String user, String userToUpdate) {
        // TODO
        return true;
    }
}
