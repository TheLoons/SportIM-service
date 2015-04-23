package org.sportim.service.ultimatefrisbee;

import org.apache.log4j.Logger;
import org.sportim.service.api.TableAPI;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.stats.AbstractTeamResultsBean;
import org.sportim.service.ultimatefrisbee.beans.UltimateTeamResultsBean;
import org.sportim.service.util.APIUtils;
import org.sportim.service.util.ConnectionManager;
import org.sportim.service.util.ConnectionProvider;
import org.sportim.service.util.PrivilegeUtil;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * The table (league standings) API for Ultimate Frisbee.
 *
 * See {@link org.sportim.service.api.TableAPI} for additional info.
 */
@Path("/table")
public class UltimateFrisbeeTableAPI implements TableAPI {
    private static Logger logger = Logger.getLogger(UltimateFrisbeeTableAPI.class.getName());
    private ConnectionProvider provider;

    public UltimateFrisbeeTableAPI() {
        provider = ConnectionManager.getInstance();
    }

    public UltimateFrisbeeTableAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @GET
    @Produces("application/json")
    public ResponseBean getTableForEvents(@BeanParam List<Integer> events, @HeaderParam("token") final String token) {
        for (int i : events) {
            if (!PrivilegeUtil.hasEventView(token, i)) {
                return new ResponseBean(401, "Not authorized");
            }
        }

        SortedSet<AbstractTeamResultsBean> results = getTableForEvents(events);
        if (results == null) {
            return new ResponseBean(500, "Unable to get table results.");
        }
        ResponseBean resp = new ResponseBean(200, "");
        resp.setTournamentResults(getTableForEvents(events));
        return resp;
    }

    public SortedSet<AbstractTeamResultsBean> getTableForEvents(List<Integer> events) {
        SortedSet<AbstractTeamResultsBean> table = new TreeSet<>();

        Connection conn = null;

        Map<Integer, Map<Integer, Integer>> eventResults = new HashMap<Integer, Map<Integer, Integer>>();
        Map<Integer, UltimateTeamResultsBean> teamResults = new HashMap<Integer, UltimateTeamResultsBean>();
        try {
            conn = provider.getConnection();

            // Collect the team point and event results
            collectPointResults(teamResults, eventResults, events, conn);

            // Collect points against and transfer event wins to reams
            transferEventResults(teamResults, eventResults);

            // Add results to the table, then update ranks
            for (UltimateTeamResultsBean team : teamResults.values()) {
                table.add(team);
            }

            int rank = 0;
            for (AbstractTeamResultsBean team : table) {
                team.rank = ++rank;
            }
        } catch (Exception e) {
            logger.error("Unable to get ultimate table: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            table = null;
        }
        finally {
            APIUtils.closeResources(conn);
        }
        return table;
    }

    private PreparedStatement createPointsQuery(List<Integer> events, Connection conn) throws SQLException {
        String query = APIUtils.createParamString(events.size());
        query = "SELECT eventID, teamID, SUM(points) FROM UltimateStats WHERE eventID IN (" + query + ") " +
                "GROUP BY eventID, teamID";

        PreparedStatement stmt = conn.prepareStatement(query);
        int idx = 0;
        for (Integer id : events) {
            stmt.setInt(++idx, id);
        }
        return stmt;
    }

    /**
     * Calculate the table rankings
     * @param eventResults the map from event IDs to event winners (team IDs)
     * @param teamResults the map from team IDs to team result beans
     */
    private void collectPointResults(Map<Integer, UltimateTeamResultsBean> teamResults,
                                     Map<Integer, Map<Integer, Integer>> eventResults, List<Integer> events,
                                     Connection conn) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createPointsQuery(events, conn);
            rs = stmt.executeQuery();
            // Get all teams so we can fill in with zeros if needed
            Set<Integer> allTeamsInTournament = getAllTeamsInTournament(events);

            while (rs.next()) {
                int eventID = rs.getInt(1);
                int teamID = rs.getInt(2);
                int points = rs.getInt(3);

                // Increment team point count
                UltimateTeamResultsBean teamRes = teamResults.get(teamID);
                if (teamRes == null) {
                    teamRes = new UltimateTeamResultsBean(teamID);
                    teamResults.put(teamID, teamRes);
                    allTeamsInTournament.remove(teamID);
                }
                teamRes.pointsFor += points;
                teamRes.pointsAgainst = 0;

                // Record event result
                Map<Integer, Integer> eventResult = eventResults.get(eventID);
                if (eventResult == null) {
                    eventResult = new HashMap<Integer, Integer>();
                    eventResults.put(eventID, eventResult);
                }
                Integer eventPoints = eventResult.get(teamID);
                if (eventPoints == null) {
                    eventResult.put(teamID, points);
                } else {
                    eventPoints += points;
                    eventResult.put(teamID, eventPoints);
                }
            }

            for (int team : allTeamsInTournament) {
                UltimateTeamResultsBean teamRes = new UltimateTeamResultsBean(team);
                teamRes.pointsFor = 0;
                teamRes.pointsAgainst = 0;
                teamResults.put(team, teamRes);
            }
        } catch (Exception e) {
            logger.error("Unable to get ultimate table: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(rs, stmt);
        }
    }

    /**
     * Collate the results of all of the events
     * @param eventResults map from event ID to event winners (will be filled)
     * @param teamResults map from team ID to team results beans (will be filled)
     * @param rs the result set from the event result query
     * @param events the list of event IDs
     * @throws Exception
     */
    private void transferEventResults(Map<Integer, UltimateTeamResultsBean> teamResults,
                                      Map<Integer, Map<Integer, Integer>> eventResults) {
        for (Map<Integer, Integer> eventResult : eventResults.values()) {
            int maxTeam = -1;
            int maxScore = -1;
            Set<Integer> ties = new HashSet<Integer>();
            for (Integer teamID : eventResult.keySet()) {
                if (eventResult.get(teamID) > maxScore) {
                    maxScore = eventResult.get(teamID);
                    maxTeam = teamID;
                    ties.clear();
                } else if (eventResult.get(teamID) == maxScore) {
                    ties.add(maxTeam);
                    ties.add(teamID);
                }
                for (Integer otherTeamID : eventResult.keySet()) {
                    if (otherTeamID != teamID) {
                        UltimateTeamResultsBean teamRes = teamResults.get(otherTeamID);
                        if (teamRes != null) {
                            teamRes.pointsAgainst += eventResult.get(teamID);
                        }
                    }
                }
            }
            if (!ties.isEmpty()) {
                for (Integer teamID : ties) {
                    UltimateTeamResultsBean teamRes = teamResults.get(teamID);
                    if (teamRes != null) {
                        teamRes.ties += 1;
                    }
                }
            } else {
                UltimateTeamResultsBean teamRes = teamResults.get(maxTeam);
                if (teamRes != null) {
                    teamRes.wins += 1;
                }

                for (Integer otherTeamID : eventResult.keySet()) {
                    if (otherTeamID != maxTeam) {
                        teamRes = teamResults.get(otherTeamID);
                        if (teamRes != null) {
                            teamRes.losses += 1;
                        }
                    }
                }
            }
        }
    }

    private Set<Integer> getAllTeamsInTournament(List<Integer> events) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet allTeamsRs = null;
        Set<Integer> allTeamsInTournament = new HashSet<Integer>();
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT DISTINCT(TeamId) FROM TeamEvent " +
                    "WHERE EventId IN (" + APIUtils.createParamString(events.size()) + ")");
            int i = 0;
            for (int id : events) {
                stmt.setInt(++i, id);
            }
            allTeamsRs = stmt.executeQuery();
            while (allTeamsRs.next()) {
                allTeamsInTournament.add(allTeamsRs.getInt(1));
            }
        } catch (Exception e) {
            logger.error("Unable to get teams in tournament: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            return null;
        } finally {
            APIUtils.closeResources(allTeamsRs, stmt, conn);
        }
        return allTeamsInTournament;
    }
}
