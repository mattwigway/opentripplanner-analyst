package org.opentripplanner.analyst.core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicTile extends Tile {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicTile.class);
    final SampleSource ss;
    
    DynamicTile(TileRequest req, SampleSource sampleSource) {
        super(req);
        this.ss = sampleSource;
    }
    
    public BufferedImage generateImage(ShortestPathTree spt) {
        long t0 = System.currentTimeMillis();
        BufferedImage image = new BufferedImage(width, height, 
                                                //BufferedImage.TYPE_BYTE_INDEXED,
                                                //DEFAULT_COLOR_MAP);
                                                BufferedImage.TYPE_BYTE_GRAY);
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
                    Sample s = ss.getSample(lon, lat);
                    if (s == null)
                        continue;
                    byte pixel = s.evalByte(spt);
                    if (pixel >= 150)
                        continue;
                    // do not use grid locations from sample, they are binned
                    int index = gx + gy * width;
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
