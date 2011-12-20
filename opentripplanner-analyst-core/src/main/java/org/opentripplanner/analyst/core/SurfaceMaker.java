package org.opentripplanner.analyst.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.geotools.geometry.GeometryBuilder;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.coordinate.GeometryFactory;
import org.opengis.geometry.coordinate.Position;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.common.geometry.HashGrid;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.geotools.referencing.crs.DefaultGeographicCRS;


public class SurfaceMaker {

    private static final Logger LOG = LoggerFactory.getLogger(SurfaceMaker.class);
    GeometryBuilder builder;
    GeometryFactory factory;
    Set<Position> posts = new HashSet<Position>(1000);
    
    public SurfaceMaker() {
        try {
            builder = new GeometryBuilder(DefaultGeographicCRS.WGS84_3D);
            factory = builder.getGeometryFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOG.debug("adding posts...");
        for (double lon=0; lon<1; lon += 0.01) {
            for (double lat=0; lat<1; lat += 0.01) {
                posts.add(new Pos(lon, lat));
            }
        }
    }
    
    public void test() {
        
        HashGrid<Vertex> hashGrid = new HashGrid<Vertex>(100, 400, 400);
        File graphFile = new File("/home/syncopate/otp_data/pdx/Graph.obj");
        Graph g;
        try {
            g = Graph.load(graphFile, Graph.LoadLevel.FULL);
        } catch (Exception e) {
            LOG.error("failed to read graph.");
            return;
        }
        LOG.debug("inserting all street vertices into hashgrid...");
        for (Vertex v : g.getVertices()) {
            if (v instanceof StreetVertex) {
                hashGrid.put(v);
            }
        }
        LOG.debug(hashGrid.toStringVerbose());
        LOG.debug("shuffling vertex list...");
        List<Vertex> cv = new ArrayList<Vertex>(g.getVertices());
        Collections.shuffle(cv);
        int count = 0;
        LOG.debug("querying...");
        long t0 = System.currentTimeMillis();
        long dummy = 0;
        for (Vertex v : cv) {
            Vertex closest = hashGrid.closest(v, 100);
            if (closest != null)
                dummy += closest.getIndex(); // prevent JVM from optimizing out calls
            if (count >= 100000) 
                break;
            //System.out.printf("vertex %s closest %s (%f meters)\n", v, closest, v.distance(closest));
            count += 1;
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("{} queries, average query time: {} msec", count, (t1 - t0)/((double)count));
        LOG.debug("Meaningless output: " + dummy);
    }
    
    public static void main(String[] args) {
        SurfaceMaker maker = new SurfaceMaker();
        maker.test();
    }
    
    private static class Pos implements Position, DirectPosition {

        private static final int DIM = 3;
    
        double[] coords = new double[DIM];

        public Pos(double x, double y) {
            coords[0] = x;
            coords[1] = y;
            coords[2] = 100 * (x + y);
        }
        
        @Override
        public double[] getCoordinate() {
            return coords;
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return DefaultGeographicCRS.WGS84_3D;
        }

        @Override
        public int getDimension() {
            return DIM;
        }

        @Override
        public double getOrdinate(int index) throws IndexOutOfBoundsException {
            return coords[index];
        }

        @Override
        public void setOrdinate(int index, double value)
                throws IndexOutOfBoundsException, UnsupportedOperationException {
            coords[index] = value;
        }

        @Override
        public DirectPosition getDirectPosition() {
            return this;
        }

    }
}
