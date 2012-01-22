package org.opentripplanner.analyst.request;

import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.core.Tile.Sample;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;

public class SPTCacheLoader extends CacheLoader<SPTRequest, ShortestPathTree> {

    private static final Logger LOG = LoggerFactory.getLogger(SPTCacheLoader.class);
    private static GraphService graphService;
    
    // TODO: switch to Spring IOC
    public static void setGraphService(GraphService gs) {
      graphService = gs;
    }
    
    @Override
    public ShortestPathTree load(SPTRequest req) throws Exception {
        LOG.debug("spt cache miss : {}", req);
        // kludge
        Sample s = Tile.makeSample(0, 0, req.lon, req.lat);
        if (s == null) {
            LOG.debug(" no origin vertex found");
            return null;
        }
        Vertex origin = s.v0;
        TraverseOptions options = getOptions(origin, req.time);
        State initialState = new State(req.time, origin, options);
        LOG.debug("initial state: {}", initialState);
        GenericDijkstra dijkstra = new GenericDijkstra(options);
        long t0 = System.currentTimeMillis();
        ShortestPathTree spt = dijkstra.getShortestPathTree(initialState);
        long t1 = System.currentTimeMillis();
        LOG.debug("calculated spt in {}msec", (int)(t1-t0));
        return spt;
    }

    private static TraverseOptions getOptions(Vertex origin, long t) {
        Graph graph = graphService.getGraph();
        TraverseOptions options = new TraverseOptions();
        // genericDijkstra asks for a traverseoptions, but state contains one now...
        options.setCalendarService(graphService.getCalendarService());
        // must set calendar service before setting service days
        options.setServiceDays(t);
        options.setMaxWalkDistance(30000);
        options.setTransferTable(graph.getTransferTable());
        return options;
    }    
    
}
