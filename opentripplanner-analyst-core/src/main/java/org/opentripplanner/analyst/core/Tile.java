package org.opentripplanner.analyst.core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TurnVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;

public class Tile {

    /* STATIC */
    private static final Logger LOG = LoggerFactory.getLogger(Tile.class);
    private static Graph graph;
    private static STRtree index;
    private static final IndexColorModel DEFAULT_COLOR_MAP = getDefaultColorMap();
    private static final GeometryFactory factory = new GeometryFactory();
    private static final double SEARCH_RADIUS_M = 100; // meters
    private static final double SEARCH_RADIUS_DEG = 
            DistanceLibrary.metersToDegrees(SEARCH_RADIUS_M);

    // this should really be handled by graph-specific TileFactories not global state
    public static void setGraphService(GraphService gs) {
        graph = gs.getGraph();
        // build a spatial index of road geometries (not individual edges)
        index = new STRtree();
        for (TurnVertex tv : IterableLibrary.filter(graph.getVertices(), TurnVertex.class)) {
            if (! tv.getPermission().allows(StreetTraversalPermission.PEDESTRIAN))
                continue;
            Geometry geom = tv.getGeometry();
            index.insert(geom.getEnvelopeInternal(), tv);
        }
        index.build();
        LOG.debug("spatial index size: {}", index.size());
    }
    
    /* INSTANCE */
    final GridGeometry2D gg;
    final int width, height;
    List<Sample> samples = new ArrayList<Sample>();
    
    public Tile(TileRequest req) {
        GridEnvelope2D gridEnv = new GridEnvelope2D(0, 0, req.width, req.height);
        this.gg = new GridGeometry2D(gridEnv, (org.opengis.geometry.Envelope)(req.bbox));
        // TODO: check that gg intersects graph area 
        this.samples = new ArrayList<Sample>();
        LOG.debug("preparing tile for {}", gg.getEnvelope2D());
        // Envelope2D worldEnv = gg.getEnvelope2D();
        this.width = gridEnv.width;
        this.height = gridEnv.height;
        CoordinateReferenceSystem crs = gg.getCoordinateReferenceSystem2D(); 
        try {
            MathTransform tr = CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84);
            // grid coordinate object to be reused for examining each cell 
            GridCoordinates2D coord = new GridCoordinates2D();
            for (int gy = 0; gy < height; gy++) {
                if (gy % 100 == 0)
                    LOG.trace("raster line {} / {}", gy, height);
                for (int gx = 0; gx < width; gx++) {
                    coord.x = gx;
                    coord.y = gy;
                    // find coordinates for current raster cell in tile CRS
                    DirectPosition sourcePos = gg.gridToWorld(coord);
                    // convert coordinates in tile CRS to WGS84
                    //LOG.debug("world : {}", sourcePos);
                    tr.transform(sourcePos, sourcePos);
                    //LOG.debug("wgs84 : {}", sourcePos);
                    // axis order can vary
                    double lon = sourcePos.getOrdinate(0);
                    double lat = sourcePos.getOrdinate(1);
                    // TODO: axes are reversed in the default mathtransform
                    Sample s = makeSample(gx, gy, lon, lat);
                    if (s != null)
                        samples.add(s);
                }
            }
            LOG.debug("finished preparing tile. number of samples: {}", samples.size()); 
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return;
        }
    }
    
    private static int timeToVertex(TurnVertex v, DistanceOp o) {
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
    
    public BufferedImage generateImage(ShortestPathTree spt) {
        long t0 = System.currentTimeMillis();
        BufferedImage image = new BufferedImage(width, height, 
                BufferedImage.TYPE_BYTE_INDEXED, DEFAULT_COLOR_MAP);
        byte[] imagePixelData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        Arrays.fill(imagePixelData, (byte)255);
        for (Sample s : samples) {
            byte pixel = s.eval(spt);
            if (pixel > 150)
                continue;
            int index = s.x + s.y * width;
            imagePixelData[index] = pixel;
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("filled in tile image from SPT in {}msec", t1 - t0);
        return image;
    }
    
    public static class Sample {
        public final int x, y, t0, t1;
        public final Vertex v0, v1;
        Sample (int x, int y, Vertex v0, int t0, Vertex v1, int t1) {
            this.x = x;
            this.y = y;
            this.v0 = v0;
            this.t0 = t0;
            this.v1 = v1;
            this.t1 = t1;
        }
                
        public byte eval(ShortestPathTree spt) {
            State s0 = spt.getState(v0);
            State s1 = spt.getState(v1);
            long m0 = 255;
            long m1 = 255;
            if (s0 != null)
                m0 = (s0.getElapsedTime() + t0) / 60; 
            if (s1 != null)
                m1 = (s1.getElapsedTime() + t1) / 60; 
            if (m1 < m0)
                m0 = m1;
            if (m0 >= 255)
                m0 = 255;
            return (byte) m0;
        }
    }
    
    public static Sample makeSample(int x, int y, double lon, double lat) {
        Coordinate c = new Coordinate(lon, lat);
        Point p = factory.createPoint(c);
        
        // track best two turn vertices
        TurnVertex v0 = null;
        TurnVertex v1 = null;
        DistanceOp o0 = null;
        DistanceOp o1 = null;
        double d0 = Double.MAX_VALUE;
        double d1 = Double.MAX_VALUE;

        // query
        Envelope env = new Envelope(c);
        env.expandBy(SEARCH_RADIUS_DEG, SEARCH_RADIUS_DEG);
        @SuppressWarnings("unchecked")
        List<TurnVertex> vs = (List<TurnVertex>) index.query(env);
        // query always returns a (possibly empty) list, but never null
        
        // find two closest among nearby geometries
        for (TurnVertex v : vs) {
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
            Sample s = new Sample(x, y, v0, t0, v1, t1);
            return s;
        }
        return null;
    }

    private static IndexColorModel getDefaultColorMap() {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        byte[] a = new byte[256];
        Arrays.fill(a, (byte)0);
        for (int i=0; i<30; i++) {
            g[i + 00]  =  // <  30 green 
            a[i + 00]  =  
            b[i + 30]  =  // >= 30 blue
            a[i + 30]  =  
            g[i + 60]  =  // >= 60 yellow 
            r[i + 60]  =
            a[i + 60]  =  
            r[i + 90]  =  // >= 90 red
            a[i + 90]  =  
            b[i + 120] =  // >=120 pink fading to transparent 
            a[i + 120] =  
            r[i + 120] = (byte) (255 - (42 - i) * 6);
        }
        return new IndexColorModel(8, 256, r, g, b, a);
    }

    private static IndexColorModel getAlternateColorMap() {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        byte[] a = new byte[256];
        Arrays.fill(a, (byte)255);
        for (int i=0; i<30; i++) {
            byte u = (byte) (i * 8);
            byte d = (byte) (255 - u);
            g[i + 00]  = d; // <  30 green 
            b[i + 00]  = u; 
            b[i + 30]  = d; // >= 30 blue
            g[i + 30]  = u; 
            r[i + 30]  = u; 
            g[i + 60]  = d; // >= 60 yellow 
            r[i + 60]  = d;
            r[i + 60] += u;
            r[i + 90]  = d; // >= 90 red
            b[i + 90]  = u;
            r[i + 90] += u;
            b[i + 120] = d; // >=120 pink fading to transparent 
            r[i + 120] = d;
            a[i + 120] = d;
        }
        Arrays.fill(a, 149, 255, (byte)0);
        return new IndexColorModel(8, 256, r, g, b, a);
    }

    public GridCoverage2D getGridCoverage2D(ShortestPathTree spt) {
        BufferedImage image = generateImage(spt);
        com.vividsolutions.jts.geom.Envelope graphEnvelope = graph.getExtent();
        org.opengis.geometry.Envelope graphRange = new Envelope2D(
                DefaultGeographicCRS.WGS84, 
                graphEnvelope.getMinX(),  graphEnvelope.getMinY(), 
                graphEnvelope.getWidth(), graphEnvelope.getHeight());
        GridCoverage2D gridCoverage = new GridCoverageFactory().create(
                "isochrone", image, graphRange);
        return gridCoverage;
    }
    
}
