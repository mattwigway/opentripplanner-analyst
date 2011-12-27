package org.opentripplanner.analyst.core;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.geometry.HashGrid;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class VertexRaster {

    /* STATIK */
    private static final Logger LOG = LoggerFactory.getLogger(VertexRaster.class);

    private static Graph graph;
    private static HashGrid<Vertex> hashGrid;
    private static double minLon, minLat, maxLon, maxLat, avgLon, avgLat;
    private static double widthMeters,  heightMeters;
    private static double widthDegrees, heightDegrees;
    
    /* INSTANCE */
    double resolutionMeters;
    double lonPitch, latPitch;
    int widthPixels, heightPixels;
    List<Sample> samples = new ArrayList<Sample>();
    
    // should really be handled by a VertexRasterFactory not global state
    public static void setGraph(Graph g) {
        graph = g;
        Envelope env = graph.getExtent();
        minLon = env.getMinX();
        maxLon = env.getMaxX();
        avgLon = (minLon + maxLon) / 2;
        minLat = env.getMinY();
        maxLat = env.getMaxY();
        avgLat = (minLat + maxLat) / 2;
        heightMeters  = DistanceLibrary.distance(minLat, avgLon, maxLat, avgLon);
        widthMeters   = DistanceLibrary.distance(avgLat, minLon, avgLat, maxLon);
        heightDegrees = maxLat - minLat;
        widthDegrees  = maxLon - minLon;
        
        // use avglat/lon in making hashgrid
        hashGrid = new HashGrid<Vertex>(100, 400, 400);
        for (Vertex v : IterableLibrary.filter(g.getVertices(), StreetVertex.class)) {
            hashGrid.put(v);
        }
    }
    
    // actually, pixel sample point is in the center of the pixel...
    public VertexRaster(double resolutionMeters) {
        LOG.debug("preparing raster...");
        this.resolutionMeters = resolutionMeters;
        double degreesPerMeterLon = widthDegrees / widthMeters;
        double degreesPerMeterLat = heightDegrees / heightMeters;
        this.lonPitch = degreesPerMeterLon * resolutionMeters; 
        this.latPitch = degreesPerMeterLat * resolutionMeters;
        this.widthPixels  = (int) (widthDegrees  / lonPitch);
        this.heightPixels = (int) (heightDegrees / latPitch);
        // find a representative vertex for each pixel
        ArrayList<Sample> samples = new ArrayList<Sample>();
        for (int y=0; y<heightPixels; y++){
            double lat = maxLat - y * latPitch; 
            for (int x=0; x<widthPixels;  x++){
                double lon = minLon + x * lonPitch;
                Vertex v = hashGrid.closest(lon, lat, 200);
                if (v != null) {
                    int t = (int) (v.distance(new Coordinate(lon, lat)) / 1.33);
                    samples.add(new Sample(x, y, v, t));
                }
            }
        }
        samples.trimToSize();
        this.samples = samples;
        LOG.debug("finished preparing raster.");
    }

    public BufferedImage getImage(ShortestPathTree spt) {
        int alpha = 180;
        alpha <<= 24;
        BufferedImage image = new BufferedImage(widthPixels, heightPixels, BufferedImage.TYPE_INT_ARGB);
        int[] imagePixelData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        // java bytes are signed and 32 bits long anyway, use ints w range checking
        int red, green, blue;
        LOG.debug("filling in image...");
        for (Sample s : samples) {
            State state = spt.getState(s.vertex);
            if (state == null)
                continue;
            long minutes = (state.getElapsedTime() + s.time) / 60;
            if (minutes > 120)
                continue;
            int subMinutes = 30 - (int) (minutes % 30);
            red = green = blue = 0;
            if (minutes < 30) {
                green = subMinutes * 8;
            } else if (minutes < 60) {
                blue = subMinutes * 8;
            } else if (minutes < 90) {
                red = subMinutes * 8;
            } else if (minutes < 120) {
                red  = subMinutes * 8;
                blue = subMinutes * 8;
            } else {
                blue  = 150;
                green = 150;
            }
            int index = s.x + s.y * widthPixels;
            imagePixelData[index] = alpha | red << 16 | green << 8 | blue;
        }
        LOG.debug("finished filling in image.");
        return image;
    }
    
    public BufferedImage getImageIndexed(ShortestPathTree spt) {
        BufferedImage image = new BufferedImage(widthPixels, heightPixels, 
                BufferedImage.TYPE_BYTE_INDEXED, defaultColorMap());
        byte[] imagePixelData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        Arrays.fill(imagePixelData, (byte)255);
        LOG.debug("filling in image...");
        for (Sample s : samples) {
            State state = spt.getState(s.vertex);
            if (state == null)
                continue;
            long minutes = (state.getElapsedTime() + s.time) / 60;
            if (minutes > 255)
                minutes = 255;
            if (minutes > 120)
                continue;
            int index = s.x + s.y * widthPixels;
            imagePixelData[index] = (byte) minutes;
        }
        LOG.debug("finished filling in image.");
        return image;
    }

    class Sample {
        int x, y, time;
        Vertex vertex;
        Sample (int x, int y, Vertex vertex, int time) {
            this.x = x;
            this.y = y;
            this.vertex = vertex;
            this.time = time;
        }
    }

    private IndexColorModel defaultColorMap() {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        byte[] a = new byte[256];
        for (int i=0; i<30; i++) {
            // 0-30 green
            g[i + 00] = 
            // > 30 blue
            b[i + 30] =
            // > 60 yellow
            g[i + 60] = 
            r[i + 60] =
            // > 90 red
            r[i + 90] =
            // >120 pink
            b[i + 120] =
            r[i + 120] = (byte) ((30 - i) * 8);
        }
        // alpha channel
        Arrays.fill(a, (byte)190);
        // 255 is transparent
        a[255] = 0;
        return new IndexColorModel(8, 256, r, g, b, a);
    }

}
