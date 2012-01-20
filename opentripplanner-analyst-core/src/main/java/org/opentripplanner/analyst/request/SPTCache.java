package org.opentripplanner.analyst.request;

import java.util.HashMap;
import java.util.Map;

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SPTCache {

    private static final Logger LOG = LoggerFactory.getLogger(SPTCache.class);
    private static Graph graph;
    private static Map<T2<Vertex, Long>, ShortestPathTree> cache = 
            new HashMap<T2<Vertex, Long>, ShortestPathTree>();
    private static TraverseOptions options;
    
    private SPTCache() { /* do not instantiate me */ }
    
    // TODO: switch to Spring IOC
    public static void setGraphService(GraphService gs) {
      // dec 6 2011 7:45am CET
      long tripTime = 1323153900;
      graph = gs.getGraph();
      options = new TraverseOptions();
      // genericDijkstra asks for a traverseoptions, but state contains one now...
      options.setCalendarService(gs.getCalendarService());
      // must set calendar service before setting service days
      options.setServiceDays(tripTime);
      options.setMaxWalkDistance(30000);
      options.setTransferTable(graph.getTransferTable());
      
    }

    public static ShortestPathTree get(double lon, double lat, long t) {
        // kludge: Tile happens to have an STRTree in it
        Vertex v = Tile.vertexNear(lon, lat, 400);
        T2<Vertex, Long> key = new T2<Vertex, Long>(v, t);
        LOG.debug("request spt for {} {}", v, t);
        ShortestPathTree spt = cache.get(key);
        if (spt == null) {
            LOG.debug("spt cache miss");
            spt = search(v, t);
            cache.put(key, spt);
        } else {
            LOG.debug("spt cache hit");
        }
        return spt;
    }

    private static ShortestPathTree search(Vertex origin, long t) {
        State initialState = new State(t/1000, origin, options);
        LOG.debug("initial state: {}", initialState);
        GenericDijkstra dijkstra = new GenericDijkstra(options);
        long t0 = System.currentTimeMillis();
        ShortestPathTree spt = dijkstra.getShortestPathTree(initialState);
        long t1 = System.currentTimeMillis();
        LOG.debug("calculated spt in {}msec", (int)(t1-t0));
        return spt;
    }

}
