import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.Envelope;
import org.opentripplanner.analyst.core.TravelTimeImageFunction;
import org.opentripplanner.analyst.core.VertexRaster;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.geometry.HashGrid;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.OverlayGraph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.StreetVertex;

public class TestRasterAccessibility extends TestCase {

    private static Graph graph;
    private static GraphServiceImpl graphService;
    private static HashGrid<Vertex> hashGrid; 
    private static TraverseOptions options;
    private static long tripTime;

    public static void setup() throws Exception {
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
        options.setMaxWalkDistance(100000);
        options.setTransferTable(graph.getTransferTable());
    }
        
    public void XtestRaster() throws Exception {
        List<Vertex> vertices = new ArrayList<Vertex>(graph.getVertices());
        Collections.shuffle(vertices);
        for (int i = 0; i<3; i++) {
            TraverseOptions options = new TraverseOptions();
            options.setMaxWalkDistance(100000);
            // dec 10 2011 5:30pm CET
            State initialState = new State(1323534600, vertices.get(i), options);
            System.out.println(initialState);
            //genericDijkstra asks for a traverseoptions, but state contains one now...
            options.setCalendarService(graphService.getCalendarService());
            options.setTransferTable(graph.getTransferTable());
            // must set calendar service before setting service days
            options.setServiceDays(initialState.getTime());
            System.out.printf("finding spt \n");
            ShortestPathTree spt = new GenericDijkstra(options).getShortestPathTree(initialState);
            System.out.printf("preparing coverage \n");
            TravelTimeImageFunction imageFunction = new TravelTimeImageFunction(hashGrid, spt);
            //LOG.debug(hashGrid.toStringVerbose());
            // GraphRaster gRaster = new GraphRaster(graph, resolution);
            com.vividsolutions.jts.geom.Envelope graphEnvelope = graph.getExtent();
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
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
            File tiff = new File(String.format("/home/syncopate/out%d.tiff", i));
            System.out.printf("writing tiff \n");
            try {
                writer = new GeoTiffWriter(tiff);
                writer.write(gridCoverage, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.printf("done writing tiff \n");
        }
//        new GridCoverageFactory().create(
//                "name of the coverage",
//                image,  // a RenderedImage, which is implemented by BufferedImage
//                null,   // GridGeometry2D gridGeometry,
//                null,   // GridSampleDimension[] bands,
//                null,   // GridCoverage[] sources,
//                null ); // Map<?,?> properties);
//        
//        LOG.debug("shuffling vertex list...");
//        List<Vertex> cv = new ArrayList<Vertex>(g.getVertices());
//        Collections.shuffle(cv);
//        int count = 0;
//        LOG.debug("querying...");
//        long t0 = System.currentTimeMillis();
//        long t1 = System.currentTimeMillis();
//        LOG.debug("{} queries, average query time: {} msec", count, (t1 - t0)/((double)count));
    }

    @Test
    public void testVertexRaster() throws Exception {
        setup();
        VertexRaster raster = new VertexRaster(50);
        List<Vertex> vertices = new ArrayList<Vertex>(graph.getVertices());
        Collections.shuffle(vertices);
        for (int i = 0; i<10; i++) {
            State initialState = new State(tripTime, vertices.get(i), options);
            System.out.println(initialState);
            System.out.printf("finding spt \n");
            ShortestPathTree spt = new GenericDijkstra(options).getShortestPathTree(initialState);
            BufferedImage image = raster.getImage(spt);
            File outputfile = new File(String.format("/home/syncopate/out%d.png", i));
            ImageIO.write(image, "png", outputfile);
            // try indexed
            image = raster.getImageIndexed(spt);
            outputfile = new File(String.format("/home/syncopate/out%didx.png", i));
            ImageIO.write(image, "png", outputfile);
        }
    }
}
