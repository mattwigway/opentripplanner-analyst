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
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class Tile {

    /* STATIC */
    private static final Logger LOG = LoggerFactory.getLogger(Tile.class);
    private static final IndexColorModel DEFAULT_COLOR_MAP = getDefaultColorMap();
    
    /* INSTANCE */
    final GridGeometry2D gg;
    final int width, height;
    List<Sample> samples = new ArrayList<Sample>();
    
    Tile(TileRequest req, SampleSource sampleSource) {

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
                    Sample s = sampleSource.getSample(gx, gy, lon, lat);
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
    
    public BufferedImage generateImage(ShortestPathTree spt) {
        long t0 = System.currentTimeMillis();
        BufferedImage image = new BufferedImage(width, height, 
                BufferedImage.TYPE_BYTE_INDEXED, DEFAULT_COLOR_MAP);
        byte[] imagePixelData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        Arrays.fill(imagePixelData, (byte)255);
        for (Sample s : samples) {
            byte pixel = s.evalByte(spt);
            if (pixel >= 150)
                continue;
            int index = s.x + s.y * width;
            imagePixelData[index] = pixel;
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("filled in tile image from SPT in {}msec", t1 - t0);
        return image;
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

    @SuppressWarnings("unused")
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

}
