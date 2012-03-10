package org.opentripplanner.analyst.request;

import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Component
public class SPTCache extends CacheLoader<SPTRequest, ShortestPathTree> {

    private static final Logger LOG = LoggerFactory.getLogger(SPTCache.class);

    @Autowired
    private GraphService graphService;

    @Autowired
    private StreetVertexIndexServiceImpl index;

    private LoadingCache<SPTRequest, ShortestPathTree> sptCache = CacheBuilder
            .newBuilder()
            .concurrencyLevel(16)
            .maximumSize(16)
            .build(this);

    @Override   
    /** completes the abstract CacheLoader superclass */
    public ShortestPathTree load(SPTRequest req) throws Exception {
        LOG.debug("spt cache miss : {}", req);
        // use the shared OTP code
        //Vertex origin = index.getNearestPedestrianStreetVertex(req.lon, req.lat);
        Coordinate origCoord = new Coordinate(req.lon, req.lat);

        // Prevent transit stops from being used by requesting PEDESTRIAN
        // TODO: this is the way it was before, is this necessarily desirable?
        TraverseOptions traverseOptsForSnap = new TraverseOptions(TraverseMode.WALK);

        // name is null, don't care
        Vertex origin = index.getClosestVertex(origCoord, null, traverseOptsForSnap);

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
