package org.opentripplanner.analyst.rest;

import java.awt.image.BufferedImage;
import java.util.GregorianCalendar;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.core.TileFactory;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.SPTCacheLoader;
import org.opentripplanner.analyst.request.SPTRequest;
import org.opentripplanner.analyst.request.TileCacheLoader;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.analyst.rest.parameter.Layer;
import org.opentripplanner.analyst.rest.parameter.LayerList;
import org.opentripplanner.analyst.rest.parameter.MIMEImageFormat;
import org.opentripplanner.analyst.rest.parameter.Style;
import org.opentripplanner.analyst.rest.parameter.StyleList;
import org.opentripplanner.analyst.rest.parameter.WMSVersion;
import org.opentripplanner.analyst.rest.utils.TileUtils;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.sun.jersey.api.spring.Autowire;
import com.sun.jersey.spi.resource.Singleton;

@Path("wms")
@Singleton // maybe inject TileProvider (caches) at each request
@Autowire
public class WebMapService {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebMapService.class);
    private LoadingCache<SPTRequest, ShortestPathTree> sptCache; 
    private LoadingCache<TileRequest, Tile> tileCache;
    private TileFactory tileFactory = new TileFactory();
    SPTCacheLoader cacheLoader = new SPTCacheLoader();
    
    @Autowired
    @Required
    public void setGraphService(GraphService graphService) {
        System.out.println("cache loader");
        tileFactory.setGraphService(graphService);
        cacheLoader.setGraphService(graphService);
    }

    public void ensureCachesInitialized() {
        if (sptCache == null) {
            sptCache = CacheBuilder.newBuilder()
                    .concurrencyLevel(16)
                    .maximumSize(8)
                    .build(cacheLoader);
            tileCache = CacheBuilder.newBuilder()
                    .concurrencyLevel(16)
                    .softValues()
                    .build(new TileCacheLoader(tileFactory));
        }
    }
    
    @GET @Produces("image/*")
    public Response wmsGet(
           // Mandatory parameters
           @QueryParam("version") WMSVersion version,
           @QueryParam("request") String request,
           @QueryParam("layers")  LayerList layers, 
           @QueryParam("styles")  StyleList styles, 
           @QueryParam("srs")     CoordinateReferenceSystem srs,
           @QueryParam("bbox")    Envelope2D bbox, 
           @QueryParam("width")   int width, 
           @QueryParam("height")  int height, 
           @QueryParam("format")  MIMEImageFormat format,
           // Optional parameters
           @QueryParam("transparent") @DefaultValue("false") Boolean transparent,
           @QueryParam("bgcolor") @DefaultValue("0xFFFFFF") String bgcolor,
           @QueryParam("exceptions") @DefaultValue("XML") String exceptions,
           @QueryParam("time") GregorianCalendar time, 
           @QueryParam("elevation") @DefaultValue("0") Float elevation, 
           // Sample dimensions
           @QueryParam("DIM_ORIGINLON") Float originLon, 
           @QueryParam("DIM_ORIGINLAT") Float originLat,
           @QueryParam("DIM_ORIGINLONB") Float originLonB, 
           @QueryParam("DIM_ORIGINLATB") Float originLatB,
           @QueryParam("DIM_ELAPSED") Long elapsed,
           @Context UriInfo uriInfo ) { 
        
        ensureCachesInitialized();
        LOG.debug("params {}", uriInfo.getQueryParameters());
        LOG.debug("layers = {}", layers);
        LOG.debug("styles = {}", styles);
        LOG.debug("version = {}", version);
        LOG.debug("srs is : {}", srs.getName());
        LOG.debug("bbox is : {}", bbox);
        LOG.debug("search time is : {}", time);
        if (originLat == null || originLon == null) {
            LOG.warn("no origin (sample dimension) specified.");
            return Response.noContent().build();
        }

        bbox.setCoordinateReferenceSystem(srs);
        TileRequest tileRequest = new TileRequest(bbox, width, height);
        SPTRequest sptRequest = new SPTRequest(originLon, originLat, time.getTimeInMillis()/1000);
        SPTRequest sptRequest2 = null;
        
        Layer layer = layers.get(0);
        Style style = styles.get(0);
        if (layer != Layer.TRAVELTIME)
            sptRequest2 = new SPTRequest(originLonB, originLatB, time.getTimeInMillis()/1000);
        
        RenderRequest renderRequest = new RenderRequest(format, style, transparent);
        
        ShortestPathTree spt, spt2 = null;
        Tile tile;
        try {
            spt = sptCache.get(sptRequest);
            if (sptRequest2 != null)
                spt2 = sptCache.get(sptRequest2);
            tile = tileCache.get(tileRequest);
            // tile = tileFactory.makeDynamicTile(tileRequest);
        } catch (Exception ex) {
            /* this will catch null SPTs for failed searches */
            LOG.error("exception while accessing cache: {}", ex.getMessage());
            throw new RuntimeException(ex);
            // return Response.serverError().build();
        }
        
        BufferedImage image;
        switch (layer) {
        case DIFFERENCE :
            if (spt2 != null) {
                image = tile.generateImageSubtract(spt, spt2, renderRequest);
                break;
            }
        case HAGERSTRAND :
            if (spt2 != null) {
                image = tile.generateImageHagerstrand(spt, spt2, elapsed, renderRequest);
                break;
            } 
        default :
            image = tile.generateImage(spt, renderRequest);
        }
        
        return TileUtils.generateImageResponse(image, format);
    }
}