package org.opentripplanner.analyst.request;

import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.core.TileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;

public class TileCacheLoader extends CacheLoader<TileRequest, Tile> {

    private static final Logger LOG = LoggerFactory.getLogger(TileCacheLoader.class);

    TileFactory tileFactory;

    public TileCacheLoader(TileFactory tileFactory) {
        this.tileFactory = tileFactory;
    }

    public void setTileFactory(TileFactory factory) {
        this.tileFactory = factory;
    }

    @Override
    public Tile load(TileRequest req) throws Exception {
        LOG.debug("tile cache miss. build new tile.");
        return tileFactory.makeTemplateTile(req);
    }

}
