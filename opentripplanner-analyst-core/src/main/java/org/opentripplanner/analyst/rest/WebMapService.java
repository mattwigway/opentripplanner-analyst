package org.opentripplanner.analyst.rest;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import javax.imageio.ImageIO;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.analyst.core.VertexRaster;
import org.opentripplanner.analyst.rest.parameter.WMSImageFormat;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.resource.Singleton;

@Path("wms")
@Singleton
public class WebMapService {
    
    private static final Logger LOG = LoggerFactory.getLogger(Raster.class);
    private VertexRaster vertexRaster;
    private List<Vertex> vertices;
    private TraverseOptions options;
    private long tripTime;
    
    public WebMapService() {
        File graphFile = new File("/home/syncopate/otp_data/pdx/Graph.obj");
        Graph graph;
        try {
//            graph = Graph.load(graphFile, Graph.LoadLevel.FULL);
//            GraphServiceImpl graphService = new GraphServiceImpl();
//            graphService.setGraph(graph);
//            VertexRaster.setGraph(graph);    
//            vertexRaster = new VertexRaster(50);
//            vertices = new ArrayList<Vertex>(graph.getVertices());
//            Collections.shuffle(vertices);
            
//            // dec 6 2011 7:45am CET
//            tripTime = 1323153900;
//            options = new TraverseOptions();
//            //genericDijkstra asks for a traverseoptions, but state contains one now...
//            options.setCalendarService(graphService.getCalendarService());
//            // must set calendar service before setting service days
//            options.setServiceDays(tripTime);
//            options.setMaxWalkDistance(30000);
//            options.setTransferTable(graph.getTransferTable());

        } catch (Exception e) {
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
           @QueryParam("CRS")     CoordinateReferenceSystem crs,
           @QueryParam("BBOX")    Envelope2D bbox, 
           @QueryParam("WIDTH")   Integer width, 
           @QueryParam("HEIGHT")  Integer height, 
           @QueryParam("FORMAT")  WMSImageFormat format,
           // Optional parameters
           @QueryParam("TRANSPARENT") 
           @DefaultValue("false")    Boolean transparent,
           @QueryParam("BGCOLOR") 
           @DefaultValue("0xFFFFFF") String bgcolor,
           @QueryParam("EXCEPTIONS") 
           @DefaultValue("XML")      String exceptions,
           @QueryParam("TIME")       GregorianCalendar time, 
           @QueryParam("ELEVATION") 
           @DefaultValue("0")        Float elevation, 
           // Sample dimensions
           @QueryParam("DIM_ORIGINLON") 
           @DefaultValue("0")        Float originLon, 
           @QueryParam("DIM_ORIGINLAT") 
           @DefaultValue("0")        Float originLat
           ) { 
       
        
        // MapSearchRequest
        // MapTileRequest
        // MapRenderRequest
        bbox.setCoordinateReferenceSystem(crs);
        LOG.debug("crs is : {}", crs);
        LOG.debug("bbox is : {}", bbox);
        LOG.debug("search time is : {}", time);

        Vertex origin = vertexRaster.closestVertex(originLon, originLat, 400);
        State initialState = new State(time.getTimeInMillis()/1000, origin, options);
        LOG.debug("initial state: {}", initialState);

        GenericDijkstra dijkstra = new GenericDijkstra(options);
        long t0 = System.currentTimeMillis();
        ShortestPathTree spt = dijkstra.getShortestPathTree(initialState);
        long t1 = System.currentTimeMillis();
        LOG.debug("calculated spt in {}msec", (int)(t1-t0));

        Image image = vertexRaster.generateImage(spt);

        if (image != null) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                t0 = System.currentTimeMillis();
                ImageIO.write((BufferedImage) image, format.toString(), out);
                final byte[] imgData = out.toByteArray();
                final InputStream bigInputStream = new ByteArrayInputStream(imgData);
                t1 = System.currentTimeMillis();
                LOG.debug("wrote image in {}msec", (int)(t1-t0));
                ResponseBuilder rb = Response.ok(bigInputStream);
                CacheControl cc = new CacheControl();
                cc.setMaxAge(3600);
                cc.setNoCache(false);
                return rb.cacheControl(cc).build();
            } catch (final IOException e) {
                return Response.noContent().build();
            }
        }
        return Response.noContent().build();
    }

}