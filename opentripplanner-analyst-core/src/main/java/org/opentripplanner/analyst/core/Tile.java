package org.opentripplanner.analyst.core;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.Arrays;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Tile {

    /* STATIC */
    private static final Logger LOG = LoggerFactory.getLogger(Tile.class);
    public static final IndexColorModel DEFAULT_COLOR_MAP = buildDefaultColorMap();
    
    /* INSTANCE */
    final GridGeometry2D gg;
    final int width, height;
    
    Tile(TileRequest req) {
        GridEnvelope2D gridEnv = new GridEnvelope2D(0, 0, req.width, req.height);
        this.gg = new GridGeometry2D(gridEnv, (org.opengis.geometry.Envelope)(req.bbox));
        // TODO: check that gg intersects graph area 
        LOG.debug("preparing tile for {}", gg.getEnvelope2D());
        // Envelope2D worldEnv = gg.getEnvelope2D();
        this.width = gridEnv.width;
        this.height = gridEnv.height;
    }
    
    private static IndexColorModel buildDefaultColorMap() {
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
    private static IndexColorModel buildAlternateColorMap() {
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
    
    public abstract BufferedImage generateImage(ShortestPathTree spt);    

}
