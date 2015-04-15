package org.sportim.service.soccer;

import org.apache.log4j.Logger;
import org.sportim.service.api.TableAPI;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.stats.AbstractTeamResultsBean;
import org.sportim.service.soccer.beans.SoccerTeamResultsBean;
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
 * API for grabbing table results
 */
@Path("table")
public class SoccerTableAPI implements TableAPI {
    private static Logger logger = Logger.getLogger(SoccerTableAPI.class.getName());
    private ConnectionProvider provider;

    public SoccerTableAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SoccerTableAPI(ConnectionProvider provider) {
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
        PreparedStatement stmt = null;
        ResultSet rs = null;

        Map<Integer, Map<Integer, Integer>> eventResults = new HashMap<Integer, Map<Integer, Integer>>();
        Map<Integer, SoccerTeamResultsBean> teamResults = new HashMap<Integer, SoccerTeamResultsBean>();
        try {
            conn = provider.getConnection();
            stmt = createEventStatQuery(events, conn);
            rs = stmt.executeQuery();

            // Collect the team and event results
            collectResults(eventResults, teamResults, rs, events);

            // Calculate event winners and add them to the team results
            calculatePoints(eventResults, teamResults);

            // Add results to the table, then update ranks
            for (SoccerTeamResultsBean team : teamResults.values()) {
                table.add(team);
            }

            int rank = 0;
            for (AbstractTeamResultsBean team : table) {
                team.rank = ++rank;
            }
        } catch (Exception e) {
            logger.error("Unable to get soccer table results for events: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            table = null;
        }
        finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
        return table;
    }

    private PreparedStatement createEventStatQuery(List<Integer> events, Connection conn) throws SQLException {
        String query = APIUtils.createParamString(events.size());
        query = "SELECT teamID, eventID, SUM(goals), SUM(goalsagainst) FROM SoccerStats WHERE eventID IN (" + query + ") " +
                "GROUP BY teamID, eventID";

        PreparedStatement stmt = conn.prepareStatement(query);
        int idx = 0;
        for (Integer id : events) {
            stmt.setInt(++idx, id);
        }
        return stmt;
    }

    private void collectResults(Map<Integer, Map<Integer,Integer>> eventResults, Map<Integer, SoccerTeamResultsBean> teamResults,
                                ResultSet rs, List<Integer> events) throws Exception {
        // Get all teams so we can fill in with zeros if needed
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet allTeamsRs = null;
        Set<Integer> allTeamsInTournament = new HashSet<Integer>();
        Exception ex = null;
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
            ex = e;
        } finally {
            APIUtils.closeResources(allTeamsRs, stmt, conn);
            if (ex != null) {
                throw ex;
            }
        }

        while (rs.next()) {
            int teamID = rs.getInt(1);
            int eventID = rs.getInt(2);
            int goals = rs.getInt(3);
            int goalsagainst = rs.getInt(4);

            // Increment event goal count
            Map<Integer, Integer> eventRes = eventResults.get(eventID);
            if (eventRes == null) {
                eventRes = new HashMap<Integer, Integer>();
                eventResults.put(eventID, eventRes);
            }
            Integer eventGoals = eventRes.get(teamID);
            if (eventGoals == null) {
                eventGoals = 0;
            }
            eventRes.put(teamID, eventGoals + goals);

            // Increment team goal count
            SoccerTeamResultsBean teamRes = teamResults.get(teamID);
            if (teamRes == null) {
                teamRes = new SoccerTeamResultsBean(teamID);
                teamResults.put(teamID, teamRes);
                allTeamsInTournament.remove(teamID);
            }
            teamRes.goalsFor += goals;
            teamRes.goalsAgainst += goalsagainst;
        }

        for (int team : allTeamsInTournament) {
            SoccerTeamResultsBean teamRes = new SoccerTeamResultsBean(team);
            teamRes.goalsAgainst = 0;
            teamRes.goalsFor = 0;
            teamResults.put(team, teamRes);
        }
    }

    private void calculatePoints(Map<Integer, Map<Integer, Integer>> eventResults, Map<Integer, SoccerTeamResultsBean> teamResults) {
        for (Map<Integer, Integer> event : eventResults.values()) {
            Set<Integer> maxTeams = new HashSet<Integer>();
            int max = -1;
            // Calculate winner(s)
            for (int teamID : event.keySet()) {
                int score = event.get(teamID);
                if (score == max) {
                    maxTeams.add(teamID);
                }
                if (score > max) {
                    maxTeams.clear();
                    maxTeams.add(teamID);
                    max = score;
                }
            }

            // Store results
            for (int teamID : event.keySet()) {
                SoccerTeamResultsBean teamRes = teamResults.get(teamID);
                if (teamRes == null) {
                    continue;
                }

                if (maxTeams.contains(teamID) && maxTeams.size() == 1) {
                    teamRes.wins += 1;
                    teamRes.points += 3;
                } else if (maxTeams.contains(teamID)) {
                    teamRes.ties += 1;
                    teamRes.points += 1;
                } else {
                    teamRes.losses += 1;
                }
            }
        }
    }
}
