package org.sportim.service.api;

import org.sportim.service.beans.stats.AggregateEventBean;
import org.sportim.service.beans.stats.LeagueStatsBean;
import org.sportim.service.beans.stats.PlayerStatsBean;
import org.sportim.service.beans.stats.TeamStatsBean;

import java.util.Set;

public interface AggregationAPI {
    public boolean deleteEventStats(int eventID);

    public AggregateEventBean getEventStats(int eventID);

    public PlayerStatsBean getPlayerStats(String login, int teamID);

    public TeamStatsBean getTeamStats(int teamID);

    public LeagueStatsBean getLeagueStats(int leagueID);

    public int getEventWinner(int eventID, Set<Integer> losers);
}
