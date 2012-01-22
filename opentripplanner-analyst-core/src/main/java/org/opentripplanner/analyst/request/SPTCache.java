package org.opentripplanner.analyst.request;

import java.util.HashMap;
import java.util.Map;

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.geometry.HashGrid;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SPTCache {

    private static final Logger LOG = LoggerFactory.getLogger(SPTCache.class);
    private static GraphService graphService;
    private static Map<T2<Vertex, Long>, ShortestPathTree> cache = 
            new HashMap<T2<Vertex, Long>, ShortestPathTree>();
    private static HashGrid<Vertex> hashGrid;
    
    private SPTCache() { /* do not instantiate me */ }
    
    // TODO: switch to Spring IOC
    public static void setGraphService(GraphService gs) {
      graphService = gs;
      Graph graph = graphService.getGraph();
      hashGrid = new HashGrid<Vertex>(100, 400, 400);
      for (Vertex v : IterableLibrary.filter(graph.getVertices(), StreetVertex.class)) {
          hashGrid.put(v);
      }
    }

    // synchronizing keeps multiple identical searches from starting
    // but is not a good long-term solution
    public static synchronized ShortestPathTree get(double lon, double lat, long t) {
        Vertex v = hashGrid.closest(lon, lat, 400);
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
        TraverseOptions options = getOptions(origin, t);
        State initialState = new State(t, origin, options);
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
