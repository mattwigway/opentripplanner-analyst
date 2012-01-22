package org.opentripplanner.analyst.request;

import org.opentripplanner.analyst.core.Tile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;

public class TileCacheLoader extends CacheLoader<TileRequest, Tile> {

    private static final Logger LOG = LoggerFactory.getLogger(TileCacheLoader.class);

    @Override
    public Tile load(TileRequest req) throws Exception {
        LOG.debug("tile cache miss. build new tile.");
        return new Tile(req);
    }

}
