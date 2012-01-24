package org.opentripplanner.analyst.core;

import java.util.List;

import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Envelope;

public interface GeometryIndexService {
    @SuppressWarnings("rawtypes")
    List query(Envelope env);
    @SuppressWarnings("rawtypes")
    List queryPedestrian(Envelope env);

    Vertex getNearestPedestrianStreetVertex(double lon, double lat);

}
