import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.opentripplanner.common.geometry.HashGrid;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;


public class TestRasterAccessibility extends TestCase {

    public void test() {
        
        HashGrid<Vertex> hashGrid = new HashGrid<Vertex>(100, 400, 400);
        File graphFile = new File("/home/syncopate/otp_data/pdx/Graph.obj");
        Graph g;
        try {
            g = Graph.load(graphFile, Graph.LoadLevel.FULL);
        } catch (Exception e) {
            LOG.error("failed to read graph.");
            return;
        }
        LOG.debug("inserting all street vertices into hashgrid...");
        for (Vertex v : g.getVertices()) {
            if (v instanceof StreetVertex) {
                hashGrid.put(v);
            }
        }
        LOG.debug(hashGrid.toStringVerbose());
        LOG.debug("shuffling vertex list...");
        List<Vertex> cv = new ArrayList<Vertex>(g.getVertices());
        Collections.shuffle(cv);
        int count = 0;
        LOG.debug("querying...");
        long t0 = System.currentTimeMillis();
        long dummy = 0;
        for (Vertex v : cv) {
            Vertex closest = hashGrid.closest(v, 100);
            if (closest != null)
                dummy += closest.getIndex(); // prevent JVM from optimizing out calls
            if (count >= 100000) 
                break;
            //System.out.printf("vertex %s closest %s (%f meters)\n", v, closest, v.distance(closest));
            count += 1;
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("{} queries, average query time: {} msec", count, (t1 - t0)/((double)count));
        LOG.debug("Meaningless output: " + dummy);
    }
    
}
