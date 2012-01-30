package org.opentripplanner.analyst.rest.parameter;

import java.util.ArrayList;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class StyleList extends ArrayList<LayerStyle> {

    private static final long serialVersionUID = 1L;

    public StyleList(String v) {
        super();
        for (String s : v.split(",")) {
            try {
                this.add(LayerStyle.valueOf(s.toUpperCase()));
            } catch (Exception e) {
                throw new WebApplicationException(Response
                    .status(Status.BAD_REQUEST)
                    .entity("unknown layer style: " + s)
                    .build());
            }
        }
    }

}

