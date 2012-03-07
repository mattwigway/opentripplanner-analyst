package org.opentripplanner.analyst;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.services.GraphService;
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
        graph = ArtificialGraphGenerator.generateGridGraph();
        graphService = new GraphServiceImpl();
        graphService.setGraph(graph);
        setGraphService(graphService);
    }

    public void setGraphService(GraphService service) {
        graph = service.getGraph();
        
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
    public void testNothing() {
        
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
