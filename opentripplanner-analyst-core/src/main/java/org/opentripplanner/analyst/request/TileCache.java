package org.opentripplanner.analyst.request;

import org.opentripplanner.analyst.core.DynamicTile;
import org.opentripplanner.analyst.core.TemplateTile;
import org.opentripplanner.analyst.core.Tile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Component
public class TileCache extends CacheLoader<TileRequest, Tile> { 
    
    private static final Logger LOG = LoggerFactory.getLogger(TileCache.class);
    
    @Autowired
    private SampleFactory sampleFactory;
    
    @Autowired
    private SampleCache sampleCache;

    private final LoadingCache<TileRequest, Tile> tileCache = CacheBuilder
            .newBuilder()
            .concurrencyLevel(16)
            .softValues()
            .build(this);

    @Override
    /** completes the abstract CacheLoader superclass */
    public Tile load(TileRequest req) throws Exception {
        return new TemplateTile(req, sampleFactory);
        //return new DynamicTile(req, sampleCache);
    }

    /** delegate to the tile LoadingCache */
    public Tile get(TileRequest req) throws Exception {
        return tileCache.get(req);
    }
        
}
