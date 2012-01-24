package org.opentripplanner.analyst.core;

import java.util.List;

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.operation.distance.DistanceOp;

public class GeometryIndex implements GeometryIndexService {
    private static final Logger LOG = LoggerFactory.getLogger(GeometryIndex.class);

    private static final double SEARCH_RADIUS_M = 100; // meters

    private static final double SEARCH_RADIUS_DEG = DistanceLibrary
            .metersToDegrees(SEARCH_RADIUS_M);

    private STRtree pedestrianIndex;
    private STRtree index;

    private GeometryFactory geometryFactory = new GeometryFactory();

    public static void ensureIndexed(Graph graph) {
        GeometryIndexService index = graph.getService(GeometryIndexService.class);
        if (index == null) {
            graph.putService(GeometryIndexService.class, new GeometryIndex(graph));
        }
    }
    
    GeometryIndex(Graph graph) {
        // build a spatial index of road geometries (not individual edges)
        pedestrianIndex = new STRtree();
        index = new STRtree();
        for (StreetVertex tv : IterableLibrary.filter(graph.getVertices(), StreetVertex.class)) {
            Geometry geom = tv.getGeometry();
            if (tv.getPermission().allows(StreetTraversalPermission.PEDESTRIAN)) {
                pedestrianIndex.insert(geom.getEnvelopeInternal(), tv);
            }
            index.insert(geom.getEnvelopeInternal(), tv);
        }
        pedestrianIndex.build();
        LOG.debug("spatial index size: {}", pedestrianIndex.size());
    }

    @SuppressWarnings("rawtypes")
    public List queryPedestrian(Envelope env) {
        return pedestrianIndex.query(env);
    }
    
    @SuppressWarnings("rawtypes")
    public List query(Envelope env) {
        return pedestrianIndex.query(env);
    }

    @Override
    public Vertex getNearestPedestrianStreetVertex(double lon, double lat) {
        Coordinate c = new Coordinate(lon, lat);
        Point p = geometryFactory.createPoint(c);

        // track best two turn vertices
        StreetVertex closestVertex = null;
        double bestDistance = Double.MAX_VALUE;

        // query
        Envelope env = new Envelope(c);
        env.expandBy(SEARCH_RADIUS_DEG, SEARCH_RADIUS_DEG);
        @SuppressWarnings("unchecked")
        List<StreetVertex> vs = (List<StreetVertex>) pedestrianIndex.query(env);
        // query always returns a (possibly empty) list, but never null

        // find two closest among nearby geometries
        for (StreetVertex v : vs) {
            Geometry g = v.getGeometry();
            DistanceOp o = new DistanceOp(p, g);
            double d = o.distance();
            if (d > SEARCH_RADIUS_DEG)
                continue;
            if (d < bestDistance) {
                closestVertex = v;
                bestDistance = d;
            }
        }

        return closestVertex;
    }
    

}
