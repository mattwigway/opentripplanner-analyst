package org.opentripplanner.analyst.request;

import org.opentripplanner.analyst.core.GeometryIndexService;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Component
public class SPTCache extends CacheLoader<SPTRequest, ShortestPathTree> {

    private static final Logger LOG = LoggerFactory.getLogger(SPTCache.class);

    @Autowired
    private GraphService graphService;

    @Autowired
    private GeometryIndexService index;

    private LoadingCache<SPTRequest, ShortestPathTree> sptCache = CacheBuilder
            .newBuilder()
            .concurrencyLevel(16)
            .maximumSize(16)
            .build(this);

    @Override   
    /** completes the abstract CacheLoader superclass */
    public ShortestPathTree load(SPTRequest req) throws Exception {
        LOG.debug("spt cache miss : {}", req);
        Vertex origin = index.getNearestPedestrianStreetVertex(req.lon, req.lat);
        TraverseOptions options = getOptions(origin, req.time);
        State initialState = new State(req.time, origin, options);
        LOG.debug("initial state: {}", initialState);
        GenericDijkstra dijkstra = new GenericDijkstra(options);
        long t0 = System.currentTimeMillis();
        ShortestPathTree spt = dijkstra.getShortestPathTree(initialState);
        long t1 = System.currentTimeMillis();
        LOG.debug("calculated spt in {}msec", (int) (t1 - t0));
        return spt;
    }

    public ShortestPathTree get(SPTRequest req) throws Exception {
        return req == null ? null : sptCache.get(req);
    }
    
    private TraverseOptions getOptions(Vertex origin, long t) {
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
