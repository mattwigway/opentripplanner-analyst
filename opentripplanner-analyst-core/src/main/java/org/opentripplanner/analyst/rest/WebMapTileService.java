package org.opentripplanner.analyst.rest;

import java.util.GregorianCalendar;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.geotools.geometry.Envelope2D;
import org.opentripplanner.analyst.core.SlippyTile;
import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.core.TileFactory;
import org.opentripplanner.analyst.request.SPTCacheLoader;
import org.opentripplanner.analyst.request.SPTRequest;
import org.opentripplanner.analyst.request.TileCacheLoader;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.analyst.rest.utils.TileUtils;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

@Path("tile/{ot}/{ox}/{oy}/{z}/{x}/{y}.png")
//@Singleton
public class WebMapTileService {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebMapTileService.class);
    private LoadingCache<SPTRequest, ShortestPathTree> sptCache; 
    private LoadingCache<TileRequest, Tile> tileCache; 
    
    private TileFactory tileFactory = new TileFactory();
    private SPTCacheLoader cacheLoader = new SPTCacheLoader();

    @Autowired
    public void setGraphService(GraphService service) {
        tileFactory.setGraphService(service);
        cacheLoader.setGraphService(service);
    }

    public WebMapTileService() {

        sptCache = CacheBuilder.newBuilder()
                .concurrencyLevel(16)
                .maximumSize(4)
                .build(cacheLoader);
        tileCache = CacheBuilder.newBuilder()
                .concurrencyLevel(16)
                .softValues()
                .build(new TileCacheLoader(tileFactory));
    }
    
    @GET @Produces("image/png")
    public Response tileGet(
           @PathParam("ox") Float originLon, 
           @PathParam("oy") Float originLat, 
           @PathParam("ot") GregorianCalendar time, 
           @PathParam("x") int x, 
           @PathParam("y") int y, 
           @PathParam("z") int z ) { 
        
//        LOG.debug("params {}", uriInfo.getQueryParameters());
        
        if (originLat == null || originLon == null) {
            LOG.warn("no origin (sample dimension) specified.");
            return Response.noContent().build();
        }
        
//        LOG.debug("srs is : {}", srs.getName());
//        LOG.debug("bbox is : {}", bbox);
//        LOG.debug("search time is : {}", time);

        Envelope2D env = SlippyTile.tile2Envelope(x, y, z);
        TileRequest tileRequest = new TileRequest(env, 255, 255);
        SPTRequest sptRequest = new SPTRequest(originLon, originLat, time.getTimeInMillis()/1000);

        // TODO: factor out image rendering from tile/spt requests (below and in WMS
        
        ShortestPathTree spt;
        Tile tile;
        try {
            spt = sptCache.get(sptRequest);
            tile = tileCache.get(tileRequest);
        } catch (Exception ex) {
            /* this will catch null SPTs for failed searches */
            LOG.error("exception while accessing cache: {}", ex.getMessage());
            throw new RuntimeException(ex);
            //return Response.serverError().build();
        }
        
        return TileUtils.generateImageResponse(tile, spt);
    }

}