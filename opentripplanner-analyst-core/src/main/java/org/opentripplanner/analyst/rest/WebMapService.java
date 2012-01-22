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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.request.SPTCache;
import org.opentripplanner.analyst.request.TileCache;
import org.opentripplanner.analyst.rest.parameter.WMSImageFormat;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.resource.Singleton;

@Path("wms")
@Singleton
public class WebMapService {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebMapService.class);
    
    public WebMapService() {
        File graphFile = new File("/home/syncopate/otp_data/pdx/Graph.obj");
        Graph graph;
        // TODO: switch to Spring IOC
        try {
            graph = Graph.load(this.getClass().getClassLoader(), 
                    graphFile, Graph.LoadLevel.FULL);
            GraphServiceImpl graphService = new GraphServiceImpl();
            graphService.setGraph(graph);
            Tile.setGraphService(graphService);
            SPTCache.setGraphService(graphService);    
        } catch (Exception e) { // IO or class not found
            e.printStackTrace();
        }
    }
    
    @GET @Produces("image/*")
    public Response wmsGet(
           // Mandatory parameters
           @QueryParam("VERSION") String  version,
           @QueryParam("REQUEST") String  request,
           @QueryParam("LAYERS")  String  layers, 
           @QueryParam("STYLES")  String  styles, 
           @QueryParam("SRS")     CoordinateReferenceSystem crs,
           @QueryParam("BBOX")    Envelope2D bbox, 
           @QueryParam("WIDTH")   int width, 
           @QueryParam("HEIGHT")  int height, 
           @QueryParam("FORMAT")  String format,
           // Optional parameters
           @QueryParam("TRANSPARENT") @DefaultValue("false") Boolean transparent,
           @QueryParam("BGCOLOR") @DefaultValue("0xFFFFFF") String bgcolor,
           @QueryParam("EXCEPTIONS") @DefaultValue("XML") String exceptions,
           @QueryParam("TIME") GregorianCalendar time, 
           @QueryParam("ELEVATION") @DefaultValue("0") Float elevation, 
           // Sample dimensions
           @QueryParam("DIM_ORIGINLON") Float originLon, 
           @QueryParam("DIM_ORIGINLAT") Float originLat,
           @Context UriInfo uriInfo ) { 
        
        // MapSearchRequest
        // MapTileRequest -- includes Graph ref?
        // MapRenderRequest

        LOG.debug("uri {}", uriInfo.getAbsolutePath());
        LOG.debug("params {}", uriInfo.getQueryParameters());
        
        if (originLat == null || originLon == null) {
            LOG.warn("no origin (sample dimension) specified.");
            return Response.noContent().build();
        }
        
        bbox.setCoordinateReferenceSystem(crs);
        GridEnvelope2D gridEnvelope = new GridEnvelope2D(0, 0, width, height);
        GridGeometry2D gridGeometry = new GridGeometry2D(gridEnvelope, (Envelope)bbox);

        LOG.debug("crs is : {}", crs.getName());
        LOG.debug("bbox is : {}", bbox);
        LOG.debug("grid envelope is : {}", gridEnvelope);
        LOG.debug("search time is : {}", time);

        ShortestPathTree spt = SPTCache.get(originLon, originLat, time.getTimeInMillis()/1000);
        Tile tile = TileCache.get(gridGeometry);
        BufferedImage image = tile.generateImage(spt);
        
        if (image != null) {
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
                LOG.debug("response image prepared");
                return rb.cacheControl(cc).build();
            } catch (final IOException e) {
                LOG.debug("exception while perparing image : {}", e.getMessage());
                return Response.noContent().build();
            }
        }
        LOG.debug("response image is null");
        return Response.noContent().build();
    }

}