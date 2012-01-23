package org.opentripplanner.analyst.rest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.GregorianCalendar;
import javax.imageio.ImageIO;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.analyst.core.SlippyTile;
import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.request.SPTCacheLoader;
import org.opentripplanner.analyst.request.SPTRequest;
import org.opentripplanner.analyst.request.TileCacheLoader;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.sun.jersey.spi.resource.Singleton;

@Path("tile/{ot}/{ox}/{oy}/{z}/{x}/{y}.png")
//@Singleton
public class WebMapTileService {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebMapTileService.class);
    private LoadingCache<SPTRequest, ShortestPathTree> sptCache; 
    private LoadingCache<TileRequest, Tile> tileCache; 
    
    public WebMapTileService() {
        File graphFile = new File("/home/syncopate/otp_data/pdx/Graph.obj");
        Graph graph;
        // TODO: switch to Spring IOC
        try {
            graph = Graph.load(this.getClass().getClassLoader(), 
                    graphFile, Graph.LoadLevel.FULL);
            GraphServiceImpl graphService = new GraphServiceImpl();
            graphService.setGraph(graph);
            Tile.setGraphService(graphService);
            SPTCacheLoader.setGraphService(graphService);    
        } catch (Exception e) { // IO or class not found
            e.printStackTrace();
        }
        sptCache = CacheBuilder.newBuilder()
                .concurrencyLevel(16)
                .maximumSize(4)
                .build(new SPTCacheLoader());
        tileCache = CacheBuilder.newBuilder()
                .concurrencyLevel(16)
                .softValues()
                .build(new TileCacheLoader());
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
            return Response.serverError().build();
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