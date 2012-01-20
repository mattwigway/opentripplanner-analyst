package org.opentripplanner.analyst.request;

import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridGeometry2D;
import org.opentripplanner.analyst.core.Tile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileCache {
    
    private static final Logger LOG = LoggerFactory.getLogger(TileCache.class);

    private static final Map<GridGeometry2D, Tile> cache = 
            new HashMap<GridGeometry2D, Tile>();

    private TileCache() { /* do not instantiate me */ }
    
    public static Tile get(GridGeometry2D gg) {
        LOG.debug("request tile for {}", gg.getEnvelope2D());
        Tile response = cache.get(gg);
        if (response == null) {
            LOG.debug("tile cache miss");
            response = new Tile(gg);
            cache.put(gg, response);
        } else {
            LOG.debug("tile cache hit");
        }
        return response;
    }
    
}
