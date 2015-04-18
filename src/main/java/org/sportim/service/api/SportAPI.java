package org.sportim.service.api;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.util.SportType;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/sports")
public class SportAPI {

    @GET
    @Produces("application/json")
    public ResponseBean getAvailableSports() {
        ResponseBean resp = new ResponseBean(200, "");
        List<Map<String, String>> sports = new ArrayList<Map<String, String>>();
        for (SportType type : SportType.values()) {
            if (type != SportType.UNKNOWN) {
                Map<String, String> sport = new HashMap<String, String>();
                sport.put("id", type.name());
                sport.put("display", type.toString());
                sports.add(sport);
            }
        }
        resp.setSports(sports);
        return resp;
    }
}
