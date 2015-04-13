package org.sportim.service.api;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.stats.SportType;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.List;

@Path("/sports")
public class SportAPI {

    @GET
    @Produces("application/json")
    public ResponseBean getAvailableSports() {
        ResponseBean resp = new ResponseBean(200, "");
        List<String> sports = new ArrayList<String>();
        for (SportType type : SportType.values()) {
            if (type != SportType.UNKNOWN) {
                sports.add(type.toString());
            }
        }
        resp.setSports(sports);
        return resp;
    }
}
