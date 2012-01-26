package org.opentripplanner.analyst.core;

import java.util.List;

import org.opentripplanner.analyst.request.SampleCache;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.opentripplanner.routing.services.GraphService;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;

public class TileFactory implements SampleSource {   
    private static final GeometryFactory factory = new GeometryFactory();
    private static final double SEARCH_RADIUS_M = 100; // meters
    private static final double SEARCH_RADIUS_DEG = 
            DistanceLibrary.metersToDegrees(SEARCH_RADIUS_M);

    private Graph graph;
    private GeometryIndex index;
    private SampleCache sampleCache = new SampleCache(this);
    
    public void setGraphService(GraphService gs) {
        graph = gs.getGraph();
        index = new GeometryIndex(graph);
        graph.putService(GeometryIndexService.class, index);
    }
    
    public Tile makeTemplateTile(TileRequest req) {
        return new TemplateTile(req, this);
    }

    public Tile makeDynamicTile(TileRequest req) {
        return new DynamicTile(req, this.sampleCache);
    }

    /* SampleSource interface */
    @Override
    public Sample getSample(double lon, double lat) {
        Coordinate c = new Coordinate(lon, lat);
        Point p = factory.createPoint(c);
        
        // track best two turn vertices
        StreetVertex v0 = null;
        StreetVertex v1 = null;
        DistanceOp o0 = null;
        DistanceOp o1 = null;
        double d0 = Double.MAX_VALUE;
        double d1 = Double.MAX_VALUE;

        // query
        Envelope env = new Envelope(c);
        env.expandBy(SEARCH_RADIUS_DEG, SEARCH_RADIUS_DEG);
        @SuppressWarnings("unchecked")
        List<StreetVertex> vs = (List<StreetVertex>) index.query(env);
        // query always returns a (possibly empty) list, but never null
        
        // find two closest among nearby geometries
        for (StreetVertex v : vs) {
            Geometry g = v.getGeometry();
            DistanceOp o = new DistanceOp(p, g);
            double d = o.distance();
            if (d > SEARCH_RADIUS_DEG)
                continue;
            if (d < d1) {
                if (d < d0) {
                    v1 = v0;
                    o1 = o0;
                    d1 = d0;
                    v0 = v;
                    o0 = o;
                    d0 = d;
                } else {
                    v1 = v;
                    o1 = o;
                    d1 = d;
                }
            }
        }
        
        // if at least one vertex was found make a sample
        if (v0 != null) { 
            int t0 = timeToVertex(v0, o0);
            int t1 = timeToVertex(v1, o1);
            Sample s = new Sample(v0, t0, v1, t1);
            return s;
        }
        return null;
    }

    private static int timeToVertex(StreetVertex v, DistanceOp o) {
        if (v == null)
            return -1;
        GeometryLocation[] gl = o.nearestLocations();
        Geometry g = v.getGeometry();
        LocationIndexedLine lil = new LocationIndexedLine(g);
        LinearLocation ll = lil.indexOf(gl[1].getCoordinate());
        LineString beginning = (LineString) 
                lil.extractLine(lil.getStartIndex(), ll);                    
        // WRONG: using unprojected coordinates
        double lengthRatio = beginning.getLength() / g.getLength();
        double distOnStreet = v.getLength() * lengthRatio;
        double distToStreet = DistanceLibrary.distance(
                gl[0].getCoordinate(), 
                gl[1].getCoordinate());
        double dist = distOnStreet + distToStreet;
        int t = (int) (dist / 1.33);
        return t;
    }
    
}
