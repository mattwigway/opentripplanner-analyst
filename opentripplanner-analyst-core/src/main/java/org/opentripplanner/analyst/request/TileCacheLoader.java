package org.opentripplanner.analyst.request;

import org.geotools.coverage.grid.GridGeometry2D;
import org.opentripplanner.analyst.core.Tile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;

public class TileCacheLoader extends CacheLoader<GridGeometry2D, Tile> {

    private static final Logger LOG = LoggerFactory.getLogger(TileCacheLoader.class);

    @Override
    public Tile load(GridGeometry2D key) throws Exception {
        LOG.debug("tile cache miss. build new tile for {}", key.getEnvelope2D());
        return new Tile(key);
    }

}
