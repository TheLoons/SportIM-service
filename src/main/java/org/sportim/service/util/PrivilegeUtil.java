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

        if (user.equals(userToView)) {
            return true;
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
            ok = rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            // TODO log4j
            e.printStackTrace();
        } finally {
            APIUtils.closeResource(rs);
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return ok;
    }

    /**
     * Check if a user can update the given user
     * @param token the user's token
     * @param userToUpdate the user to update
     * @return true if user can update
     */
    public static boolean hasUserUpdate(String token, String userToUpdate) {
        // This is simple for now - users can only update themselves
        String user = AuthenticationUtil.validateToken(token);
        return user != null && user.equals(userToUpdate);
    }

    /**
     * Check if a user can update a league
     * @param token the user's token
     * @param leagueID the league ID
     * @return true if the user can update
     */
    public static boolean hasLeagueUpdate(String token, int leagueID) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return false;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean res = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT COUNT(LeagueId) FROM League WHERE LeagueOwner = ? AND LeagueId = ?");
            stmt.setString(1, user);
            stmt.setInt(2, leagueID);
            rs = stmt.executeQuery();
            res = rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResource(rs);
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return res;
    }

    /**
     * Check if a user can update a team
     * @param token the user's token
     * @param teamID the team ID
     * @return true if the user can update
     */
    public static boolean hasTeamUpdate(String token, int teamID) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return false;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean res = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT COUNT(TeamId) FROM Team WHERE TeamOwner = ? AND TeamId = ?");
            stmt.setString(1, user);
            stmt.setInt(2, teamID);
            rs = stmt.executeQuery();
            res = rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResource(rs);
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return res;
    }

    /**
     * Check if a user can view a team
     * @param token the user's token
     * @param teamID the team ID
     * @return true if the user can view
     */
    public static boolean hasTeamView(String token, int teamID) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return false;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean res = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT COUNT(Team.TeamId) FROM Team INNER JOIN PlaysFor " +
                    "WHERE (PlaysFor.Login = ? AND Team.TeamId = ?) OR Team.TeamOwner = ?");
            stmt.setString(1, user);
            stmt.setInt(2, teamID);
            stmt.setString(3, user);
            rs = stmt.executeQuery();
            res = rs.next() && rs.getInt(1) > 0;

            if (!res) {
                APIUtils.closeResources(rs, stmt);
                stmt = conn.prepareStatement("SELECT COUNT(Team.TeamId) " +
                        "FROM Team INNER JOIN TeamBelongsTo INNER JOIN League " +
                        "WHERE League.LeagueOwner = ? AND Team.TeamId = ?");
                stmt.setString(1, user);
                stmt.setInt(2, teamID);
                rs = stmt.executeQuery();
                res = rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
        return res;
    }

    /**
     * Check if a user can update a tournament
     * @param token the user's token
     * @param tournamentID the tournament ID
     * @return true if the user can update
     */
    public static boolean hasTournamentUpdate(String token, int tournamentID) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return false;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean res = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT COUNT(t.TournamentId) FROM Tournament t INNER JOIN League l " +
                    "ON t.LeagueId = l.LeagueId " +
                    "WHERE l.LeagueOwner = ? AND t.TournamentId = ?");
            stmt.setString(1, user);
            stmt.setInt(2, tournamentID);
            rs = stmt.executeQuery();
            res = rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResource(rs);
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return res;
    }

    /**
     * Check if a user has view rights to an event
     * @param token the user's token
     * @param eventID the event ID to check
     * @return true if the user can view
     */
    public static boolean hasEventView(String token, int eventID) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return false;
        }

        boolean res = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT COUNT(EventOwner) FROM Event WHERE EventId = ? AND EventOwner = ?");
            stmt.setInt(1, eventID);
            stmt.setString(2, user);
            rs = stmt.executeQuery();
            res = rs.next() && rs.getInt(1) > 0;

            if (!res) {
                APIUtils.closeResource(rs);
                APIUtils.closeResource(stmt);
                stmt = conn.prepareStatement("SELECT COUNT(p.Login) FROM Player p INNER JOIN PlayerEvent pe " +
                        "ON p.Login = pe.Login " +
                        "INNER JOIN Event e ON e.EventId = pe.EventId " +
                        "WHERE pe.EventId = ? AND p.Login = ?");
                stmt.setInt(1, eventID);
                stmt.setString(2, user);
                rs = stmt.executeQuery();
                res = rs.next() && rs.getInt(1) > 0;
            }
            
            if (!res) {
                APIUtils.closeResource(rs);
                APIUtils.closeResource(stmt);
                stmt = conn.prepareStatement("SELECT COUNT(p.Login) FROM Player p INNER JOIN PlaysFor pf " +
                        "ON p.Login = pf.Login " +
                        "INNER JOIN TeamEvent te ON te.TeamId = pf.TeamID " +
                        "WHERE te.EventId = ? and p.Login = ?");
                stmt.setInt(1, eventID);
                stmt.setString(2, user);
                rs = stmt.executeQuery();
                res = rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResource(rs);
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return res;
    }

    /**
     * Check if a user can update an event
     * @param token the user's token
     * @param eventID the event id
     * @return true if the user can update the event
     */
    public static boolean hasEventUpdate(String token, int eventID) {
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return false;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean res = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT COUNT(EventOwner) FROM Event WHERE EventId = ? AND EventOwner = ?");
            stmt.setInt(1, eventID);
            stmt.setString(2, user);
            rs = stmt.executeQuery();
            res = rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResource(rs);
            APIUtils.closeResource(stmt);
            APIUtils.closeResource(conn);
        }
        return res;
    }

    /**
     * Check if a user can edit stats for an event
     * @param token the user's token
     * @param eventID the event ID
     * @return true if the user can update the stats
     */
    public static boolean hasEventTracking(String token, int eventID) {
        // TODO: See issue #29. We need to update event ownership before this can be really implemented
        String user = AuthenticationUtil.validateToken(token);
        if (user == null) {
            return false;
        }
        return true;
    }
}
