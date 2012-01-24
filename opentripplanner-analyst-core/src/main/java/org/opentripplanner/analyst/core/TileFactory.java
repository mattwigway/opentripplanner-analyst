package org.opentripplanner.analyst.core;

import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.services.GraphService;

public class TileFactory {   
    private Graph graph;
    private GeometryIndex index;

    public void setGraphService(GraphService gs) {
        graph = gs.getGraph();
        index = new GeometryIndex(graph);
        graph.putService(GeometryIndexService.class, index);
    }
    
    public Tile makeTile(TileRequest req) {
        return new Tile(req, graph, index);
    }
}
