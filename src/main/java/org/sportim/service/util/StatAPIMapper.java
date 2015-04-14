package org.sportim.service.util;

import org.sportim.service.api.AggregationAPI;
import org.sportim.service.api.TableAPI;
import org.sportim.service.beans.stats.SportType;
import org.sportim.service.soccer.SoccerAggregationAPI;
import org.sportim.service.soccer.SoccerTableAPI;
import org.sportim.service.ultimatefrisbee.UltimateFrisbeeAggregationAPI;
import org.sportim.service.ultimatefrisbee.UltimateFrisbeeTableAPI;

import java.util.HashMap;
import java.util.Map;

public class StatAPIMapper {
    private Map<SportType, AggregationAPI> mainAPIMap = new HashMap<SportType, AggregationAPI>();
    private Map<SportType, TableAPI> tableAPIMap = new HashMap<SportType, TableAPI>();

    public StatAPIMapper(ConnectionProvider provider) {
        SoccerAggregationAPI soccerAPI = new SoccerAggregationAPI(provider);
        mainAPIMap.put(SportType.SOCCER, soccerAPI);

        SoccerTableAPI soccerTableAPI = new SoccerTableAPI(provider);
        tableAPIMap.put(SportType.SOCCER, soccerTableAPI);

        UltimateFrisbeeAggregationAPI frisbeeAPI = new UltimateFrisbeeAggregationAPI(provider);
        mainAPIMap.put(SportType.ULTIMATE_FRISBEE, frisbeeAPI);

        UltimateFrisbeeTableAPI frisbeeTableAPI = new UltimateFrisbeeTableAPI(provider);
        tableAPIMap.put(SportType.ULTIMATE_FRISBEE, frisbeeTableAPI);
    }

    public TableAPI getTableAPI(SportType sport) {
        return tableAPIMap.get(sport);
    }

    public AggregationAPI getMainAPI(SportType sport) {
        return mainAPIMap.get(sport);
    }
}
