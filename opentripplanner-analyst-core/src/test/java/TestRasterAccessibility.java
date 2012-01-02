import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opentripplanner.analyst.core.TravelTimeImageFunction;
import org.opentripplanner.analyst.core.VertexRaster;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.geometry.HashGrid;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.spt.MultiShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTreeFactory;
import org.opentripplanner.routing.vertextype.StreetVertex;

public class TestRasterAccessibility extends TestCase {

    private Graph graph;
    private GraphServiceImpl graphService;
    private HashGrid<Vertex> hashGrid; 
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
        
        hashGrid = new HashGrid<Vertex>(100, 400, 400);
        for (Vertex v : IterableLibrary.filter(graph.getVertices(), StreetVertex.class)) {
            hashGrid.put(v);
        }
        
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
    
    // disabled
    public void XtestImageFunction() throws Exception {
        List<Vertex> vertices = new ArrayList<Vertex>(graph.getVertices());
        Collections.shuffle(vertices);
        for (int i = 0; i < 2; i++) { 
            State initialState = new State(tripTime, vertices.get(i), options);
            System.out.println(initialState);
            System.out.printf("finding spt \n");
            ShortestPathTree spt = new GenericDijkstra(options).getShortestPathTree(initialState);
            System.out.printf("preparing coverage \n");
            TravelTimeImageFunction imageFunction = new TravelTimeImageFunction(hashGrid, spt);
            com.vividsolutions.jts.geom.Envelope graphEnvelope = graph.getExtent();
            Envelope graphRange = new Envelope2D(DefaultGeographicCRS.WGS84, 
                    graphEnvelope.getMinX(),  graphEnvelope.getMinY(), 
                    graphEnvelope.getWidth(), graphEnvelope.getHeight());
            GridEnvelope gridRange = new GridEnvelope2D(0, 0, 2000, 1000);
            GridGeometry2D gridGeometry = new GridGeometry2D(gridRange, graphRange);
            GridCoverage2D gridCoverage = new GridCoverageFactory().create(
                    "name of the coverage",
                    imageFunction,
                    gridGeometry,
                    null,
                    null);
            GeoTiffWriter writer;
            File tiff = new File(String.format("/home/syncopate/out%dfunc.tiff", i));
            System.out.printf("writing tiff \n");
            try {
                writer = new GeoTiffWriter(tiff);
                writer.write(gridCoverage, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.printf("done writing tiff \n");
        }
    }

    @Test
    public void testVertexRaster() throws Exception {
        VertexRaster raster = new VertexRaster(50);
        GenericDijkstra dijkstra = new GenericDijkstra(options);
        dijkstra.setShortestPathTreeFactory(new DijkstraOptions());
        dijkstra.setSkipTraverseResultStrategy(new DijkstraOptions());

        List<Vertex> vertices = new ArrayList<Vertex>(graph.getVertices());
        Collections.shuffle(vertices);
        for (int i = 0; i<10; i++) {
            State initialState = new State(tripTime, vertices.get(i), options);
            System.out.printf("iteration %d : origin %s \n", i, initialState);
            
            long t0 = System.currentTimeMillis();
            System.out.printf("calculating spt \n");
            ShortestPathTree spt = dijkstra.getShortestPathTree(initialState);
            long t1 = System.currentTimeMillis();
            System.out.printf("done calculating spt %dmsec\n", (int)(t1-t0));

            t0 = System.currentTimeMillis();
            BufferedImage image = raster.generateImage(spt);
            t1 = System.currentTimeMillis();
            System.out.printf("done generating raster %dmsec\n", (int)(t1-t0));

            t0 = System.currentTimeMillis();
            System.out.printf("writing png ");
            File outputfile = new File(String.format("/home/syncopate/out%d.png", i));
            ImageIO.write(image, "png", outputfile);
            t1 = System.currentTimeMillis();
            System.out.printf("%dmsec\n", (int)(t1-t0));
 
            t0 = System.currentTimeMillis();
            System.out.printf("writing gif ");
            outputfile = new File(String.format("/home/syncopate/out%d.gif", i));
            ImageIO.write(image, "gif", outputfile);
            t1 = System.currentTimeMillis();
            System.out.printf("%dmsec\n", (int)(t1-t0));

//            t0 = System.currentTimeMillis();
//            System.out.printf("writing jpeg2k ");
//            outputfile = new File(String.format("/home/syncopate/out%d.j2k", i));
//            ImageIO.write(raster.getBufferedImage(), "jpeg2000", outputfile);
//            t1 = System.currentTimeMillis();
//            System.out.printf("%dmsec\n", (int)(t1-t0));
            
//            GridCoverage2D coverage = raster.getGridCoverage2D();
//            GeoTiffWriter tiffWriter;
//            File tiff = new File(String.format("/home/syncopate/out%d.tiff", i));
//            t0 = System.currentTimeMillis();
//            System.out.printf("writing geotiff ");
//            try {
//                tiffWriter = new GeoTiffWriter(tiff);
//                tiffWriter.write(coverage, null);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            t1 = System.currentTimeMillis();
//            System.out.printf("%dmsec\n", (int)(t1-t0));

        }
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
