import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opentripplanner.analyst.core.TravelTimeImageFunction;
import org.opentripplanner.analyst.core.VertexRaster;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.spt.MultiShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTreeFactory;

public class TestRasterAccessibility extends TestCase {

    private Graph graph;
    private GraphServiceImpl graphService;
    private TraverseOptions options;
    private long tripTime;

    @Before
    public void setUp() throws Exception {
        // handle this with a graphservice
        File graphFile = new File("/home/syncopate/otp_data/pdx/Graph.obj");
        graph = Graph.load(graphFile, Graph.LoadLevel.FULL);
        graphService = new GraphServiceImpl();
        graphService.setGraph(graph);
        VertexRaster.setGraph(graph);
        
        // dec 10 2011 5:30pm CET
        tripTime = 1323534600;
        options = new TraverseOptions();
        //genericDijkstra asks for a traverseoptions, but state contains one now...
        options.setCalendarService(graphService.getCalendarService());
        // must set calendar service before setting service days
        options.setServiceDays(tripTime);
        options.setMaxWalkDistance(3000);
        options.setTransferTable(graph.getTransferTable());
        options.worstTime = tripTime + 60 * 150; // we don't display over 150 min
    }

    @Test
    public void testVertexRaster() throws Exception {
        VertexRaster raster = new VertexRaster(100);
        GenericDijkstra dijkstra = new GenericDijkstra(options);
        dijkstra.setShortestPathTreeFactory(new DijkstraOptions());
        dijkstra.setSkipTraverseResultStrategy(new DijkstraOptions());

        List<Vertex> vertices = new ArrayList<Vertex>(graph.getVertices());
        Collections.shuffle(vertices);
        for (int i = 0; i<10; i++) {
            State initialState = new State(tripTime, vertices.get(i), options);
            System.out.printf("iteration %d : origin %s \n", i, initialState);
            
            long t0 = System.currentTimeMillis();
            ShortestPathTree spt = dijkstra.getShortestPathTree(initialState);
            long t1 = System.currentTimeMillis();
            System.out.printf("calculated spt %dmsec\n", (int)(t1-t0));

            BufferedImage image = raster.generateImage(spt);
            saveImage(image, "png", i);
            saveImage(image, "gif", i);
            // jpeg2000 loading is very slow
            saveImage(image, "jpeg2000", i);
            saveImage(image, "tiff", i);
            saveGeotiff(raster.getGridCoverage2D(spt), i, "coverage");
            saveImageFunction(new TravelTimeImageFunction(raster, spt), i);
        }
    }
    
    private void saveImage(RenderedImage image, String format, int i) 
        throws IOException {
        System.out.printf("writing %s ", format);
        long t0 = System.currentTimeMillis();
        File outputfile = new File(String.format("/tmp/out%d.%s", i, format));
        ImageIO.write(image, format, outputfile);
        long t1 = System.currentTimeMillis();
        System.out.printf("%dmsec\n", (int)(t1-t0));
    }
    
    private void saveGeotiff(GridCoverage2D coverage, int i, String variant) 
        throws Exception {
        File tiff = new File(String.format("/tmp/out%d.%s.geo.tiff", i, variant));
        long t0 = System.currentTimeMillis();
        System.out.printf("writing geotiff ");
        
        GeoTiffWriteParams wp = new GeoTiffWriteParams();
        wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
        wp.setCompressionType("LZW");

        ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
        params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
        
        new GeoTiffWriter(tiff).write(coverage, (GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[1]));
        long t1 = System.currentTimeMillis();
        System.out.printf("%dmsec\n", (int)(t1-t0));
    }

    private void saveImageFunction(TravelTimeImageFunction imageFunc, int i) 
        throws Exception {
        com.vividsolutions.jts.geom.Envelope graphEnvelope = graph.getExtent();
        Envelope graphRange = new Envelope2D(DefaultGeographicCRS.WGS84, 
                graphEnvelope.getMinX(),  graphEnvelope.getMinY(), 
                graphEnvelope.getWidth(), graphEnvelope.getHeight());
        GridEnvelope gridRange = new GridEnvelope2D(0, 0, 500, 500);
        GridGeometry2D gridGeometry = new GridGeometry2D(gridRange, graphRange);
        System.out.printf("building arbitrary resolution gridcoverage via imagefunction ");
        long t0 = System.currentTimeMillis();
        GridCoverage2D gridCoverage = new GridCoverageFactory().create(
                "image function test coverage",
                imageFunc,
                gridGeometry,
                null,
                null);
        long t1 = System.currentTimeMillis();
        System.out.printf("%dmsec\n", (int)(t1-t0));
        saveGeotiff(gridCoverage, i, "func");
    }
    
    class DijkstraOptions implements SkipTraverseResultStrategy, ShortestPathTreeFactory {

        @Override
        public boolean shouldSkipTraversalResult(Vertex origin, Vertex target, State parent,
                State current, ShortestPathTree spt, TraverseOptions traverseOptions) {
            return current.getTime() > traverseOptions.worstTime;
        }

        @Override
        public ShortestPathTree create() {
            return new MultiShortestPathTree();
        }
 
    }
    
}
