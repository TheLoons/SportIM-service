package org.sportim.service.soccer;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.soccer.beans.TeamResultsBean;
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
public class SoccerTableAPI {
    private ConnectionProvider provider;

    public SoccerTableAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SoccerTableAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @GET
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseBean getTableForEvents(List<Integer> events, @HeaderParam("token") final String token) {
        for (int i : events) {
            if (!PrivilegeUtil.hasEventView(token, i)) {
                return new ResponseBean(401, "Not authorized");
            }
        }

        SortedSet<TeamResultsBean> results = getTableForEvents(events);
        if (results == null) {
            return new ResponseBean(500, "Unable to get table results.");
        }
        ResponseBean resp = new ResponseBean(200, "");
        resp.setTournamentResults(getTableForEvents(events));
        return resp;
    }

    public SortedSet<TeamResultsBean> getTableForEvents(List<Integer> events) {
        SortedSet<TeamResultsBean> table = new TreeSet<>();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        Map<Integer, Map<Integer, Integer>> eventResults = new HashMap<Integer, Map<Integer, Integer>>();
        Map<Integer, TeamResultsBean> teamResults = new HashMap<Integer, TeamResultsBean>();
        try {
            conn = provider.getConnection();
            stmt = createEventStatQuery(events, conn);
            rs = stmt.executeQuery();

            // Collect the team and event results
            collectResults(eventResults, teamResults, rs);

            // Calculate event winners and add them to the team results
            calculatePoints(eventResults, teamResults);

            // Add results to the table, then update ranks
            for (TeamResultsBean team : teamResults.values()) {
                table.add(team);
            }

            int rank = 0;
            for (TeamResultsBean team : table) {
                team.rank = ++rank;
            }
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
            table = null;
        }
        finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
        return table;
    }

    private PreparedStatement createEventStatQuery(List<Integer> events, Connection conn) throws SQLException {
        String query = "";
        for (int i = 0; i < events.size(); i++) {
            if (!query.isEmpty()) {
                query += ",";
            }
            query += "?";
        }
        query = "SELECT teamID, eventID, SUM(goals), SUM(goalsagainst) FROM SoccerStats WHERE eventID IN (" + query + ") " +
                "GROUP BY teamID, eventID";

        PreparedStatement stmt = conn.prepareStatement(query);
        int idx = 0;
        for (Integer id : events) {
            stmt.setInt(++idx, id);
        }
        return stmt;
    }

    private void collectResults(Map<Integer, Map<Integer,Integer>> eventResults, Map<Integer, TeamResultsBean> teamResults,
                                ResultSet rs) throws SQLException {
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
            TeamResultsBean teamRes = teamResults.get(teamID);
            if (teamRes == null) {
                teamRes = new TeamResultsBean(teamID);
                teamResults.put(teamID, teamRes);
            }
            teamRes.goalsFor += goals;
            teamRes.goalsAgainst += goalsagainst;
        }
    }

    private void calculatePoints(Map<Integer, Map<Integer, Integer>> eventResults, Map<Integer, TeamResultsBean> teamResults) {
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
                TeamResultsBean teamRes = teamResults.get(teamID);
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
