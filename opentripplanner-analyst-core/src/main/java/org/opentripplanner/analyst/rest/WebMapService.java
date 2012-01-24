package org.opentripplanner.analyst.rest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.GregorianCalendar;

import javax.imageio.ImageIO;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.core.TileFactory;
import org.opentripplanner.analyst.request.SPTCacheLoader;
import org.opentripplanner.analyst.request.SPTRequest;
import org.opentripplanner.analyst.request.TileCacheLoader;
import org.opentripplanner.analyst.request.TileRequest;
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
@Singleton
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
                    .maximumSize(4)
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
           @QueryParam("version") String  version,
           @QueryParam("request") String  request,
           @QueryParam("layers")  String  layers, 
           @QueryParam("styles")  String  styles, 
           @QueryParam("srs")     CoordinateReferenceSystem srs,
           @QueryParam("bbox")    Envelope2D bbox, 
           @QueryParam("width")   int width, 
           @QueryParam("height")  int height, 
           @QueryParam("format")  String format,
           // Optional parameters
           @QueryParam("transparent") @DefaultValue("false") Boolean transparent,
           @QueryParam("bgcolor") @DefaultValue("0xFFFFFF") String bgcolor,
           @QueryParam("exceptions") @DefaultValue("XML") String exceptions,
           @QueryParam("time") GregorianCalendar time, 
           @QueryParam("elevation") @DefaultValue("0") Float elevation, 
           // Sample dimensions
           @QueryParam("DIM_ORIGINLON") Float originLon, 
           @QueryParam("DIM_ORIGINLAT") Float originLat,
           @Context UriInfo uriInfo ) { 
        
        ensureCachesInitialized();
        // MapSearchRequest
        // MapTileRequest -- includes Graph ref?
        // MapRenderRequest

        LOG.debug("params {}", uriInfo.getQueryParameters());
        
        if (originLat == null || originLon == null) {
            LOG.warn("no origin (sample dimension) specified.");
            return Response.noContent().build();
        }
        
//        LOG.debug("srs is : {}", srs.getName());
//        LOG.debug("bbox is : {}", bbox);
//        LOG.debug("search time is : {}", time);

        bbox.setCoordinateReferenceSystem(srs);
        TileRequest tileRequest = new TileRequest(bbox, width, height);
        SPTRequest sptRequest = new SPTRequest(originLon, originLat, time.getTimeInMillis()/1000);

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
        
        BufferedImage image = tile.generateImage(spt);
        if (image == null) {
            LOG.warn("response image is null");
            return Response.noContent().build();
        }
            
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            long t0 = System.currentTimeMillis();
            ImageIO.write(image, "png", out);
            final byte[] imgData = out.toByteArray();
            final InputStream bigInputStream = new ByteArrayInputStream(imgData);
            long t1 = System.currentTimeMillis();
            LOG.debug("wrote image in {}msec", (int)(t1-t0));
            ResponseBuilder rb = Response.ok(bigInputStream);
            CacheControl cc = new CacheControl();
            cc.setMaxAge(3600);
            cc.setNoCache(false);
            return rb.cacheControl(cc).build();
        } catch (final IOException e) {
            LOG.error("exception while perparing image : {}", e.getMessage());
            return Response.serverError().build();
        }
    }

}