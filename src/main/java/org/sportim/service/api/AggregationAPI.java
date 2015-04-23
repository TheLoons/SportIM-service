package org.sportim.service.api;

import org.sportim.service.beans.stats.AggregateEventBean;
import org.sportim.service.beans.stats.LeagueStatsBean;
import org.sportim.service.beans.stats.PlayerStatsBean;
import org.sportim.service.beans.stats.TeamStatsBean;

import java.util.Set;

/**
 * Interface that should be implemented by sports-specific aggregation APIs
 */
public interface AggregationAPI {

    /**
     * Delete an event's statistics
     * @param eventID the event ID
     * @return true if successful, false otherwise
     */
    public boolean deleteEventStats(int eventID);

    /**
     * Get an event's statistics
     * @param eventID the event ID
     * @return the AggregateEventBean containing the event results or null if the event wasn't found.
     */
    public AggregateEventBean getEventStats(int eventID);

    /**
     * Get a player's statistics
     * @param login the player's login
     * @param teamID optional team ID to constrain results (get player's stats for only that team)
     * @return the PlayerStatsBean containing the player statistics or null if the player wasn't found.
     */
    public PlayerStatsBean getPlayerStats(String login, int teamID);

    /**
     * Get a team's statistics
     * @param teamID the team ID
     * @return the TeamStatsBean containing the team's results or null if the team wasn't found.
     */
    public TeamStatsBean getTeamStats(int teamID);

    /**
     * Get a league's statistics
     * @param leagueID the league ID
     * @return the LeagueStatsBean or null if the league wasn't found.
     */
    public LeagueStatsBean getLeagueStats(int leagueID);

    /**
     * Find the winning team for an event
     * @param eventID the event ID
     * @param losers an OUT parameter, filled with the set of teams who did NOT win the event, but were participants
     * @return the ID of the winning team or 0 if the event was not found.
     */
    public int getEventWinner(int eventID, Set<Integer> losers);
}
