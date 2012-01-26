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

public class DynamicTile {

    /* STATIC */
    private static final Logger LOG = LoggerFactory.getLogger(DynamicTile.class);
    private static final IndexColorModel DEFAULT_COLOR_MAP = Tile.getDefaultColorMap();
    
    /* INSTANCE */
    final SampleSource ss;
    final GridGeometry2D gg;
    final int width, height;
    
    DynamicTile(TileRequest req, SampleSource sampleSource) {
        GridEnvelope2D gridEnv = new GridEnvelope2D(0, 0, req.width, req.height);
        this.gg = new GridGeometry2D(gridEnv, (org.opengis.geometry.Envelope)(req.bbox));
        this.width = gridEnv.width;
        this.height = gridEnv.height;
        this.ss = sampleSource;
    }
    
    public BufferedImage generateImage(ShortestPathTree spt) {
        long t0 = System.currentTimeMillis();
        BufferedImage image = new BufferedImage(width, height, 
                BufferedImage.TYPE_BYTE_INDEXED, DEFAULT_COLOR_MAP);
        byte[] imagePixelData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        Arrays.fill(imagePixelData, (byte)255);
        CoordinateReferenceSystem crs = gg.getCoordinateReferenceSystem2D();
        try {
            MathTransform tr = CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84);
            // grid coordinate object to be reused for examining each cell 
            GridCoordinates2D coord = new GridCoordinates2D();
            int ns = 0;
            for (int gy = 0; gy < height; gy++) {
                for (int gx = 0; gx < width; gx++) {
                    coord.x = gx;
                    coord.y = gy;
                    // find coordinates for current raster cell in tile CRS
                    DirectPosition sourcePos = gg.gridToWorld(coord);
                    // convert coordinates in tile CRS to WGS84
                    tr.transform(sourcePos, sourcePos);
                    // axis order can vary
                    double lon = sourcePos.getOrdinate(0);
                    double lat = sourcePos.getOrdinate(1);
                    Sample s = ss.getSample(gx, gy, lon, lat);
                    if (s == null)
                        continue;
                    byte pixel = s.evalByte(spt);
                    if (pixel >= 150)
                        continue;
                    int index = s.x + s.y * width;
                    imagePixelData[index] = pixel;
                    ns++;
                }
            }
            LOG.debug("finished preparing tile. number of samples: {}", ns); 
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return null;
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("filled in tile image from SPT in {}msec", t1 - t0);
        return image;
    }
    
}
