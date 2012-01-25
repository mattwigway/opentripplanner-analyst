package org.opentripplanner.analyst.rest;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.opentripplanner.analyst.core.GeometryIndex;
import org.opentripplanner.analyst.core.VertexRaster;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Path("raster")
//@Singleton
public class Raster {
    
    private static final Logger LOG = LoggerFactory.getLogger(Raster.class);
    private VertexRaster vertexRaster;
    private List<Vertex> vertices;
    private TraverseOptions options;
    private long tripTime;
    
    public void setGraphService(GraphService graphService) {
        Graph graph = graphService.getGraph();
        GeometryIndex.ensureIndexed(graph);
        vertexRaster = new VertexRaster(graph, 50);
        vertices = new ArrayList<Vertex>(graph.getVertices());
        Collections.shuffle(vertices);
            
        // dec 6 2011 7:45am CET
        tripTime = 1323153900;
        options = new TraverseOptions();
        //genericDijkstra asks for a traverseoptions, but state contains one now...
        options.setCalendarService(graphService.getCalendarService());
        // must set calendar service before setting service days
        options.setServiceDays(tripTime);
        options.setMaxWalkDistance(30000);
        options.setTransferTable(graph.getTransferTable());
    }
    
    @GET @Produces("image/png")
    public Response getRaster(
           @QueryParam("fromLat") Float lat,  
           @QueryParam("fromLon") Float lon,  
           @QueryParam("startDate") String startDate ) {
        
        GenericDijkstra dijkstra = new GenericDijkstra(options);
        // do not parse date for now
        Vertex origin = vertexRaster.closestVertex(lon, lat);
        State initialState = new State(tripTime, origin, options);
        LOG.debug("initial state: {}", initialState);
            
        long t0 = System.currentTimeMillis();
        ShortestPathTree spt = dijkstra.getShortestPathTree(initialState);
        long t1 = System.currentTimeMillis();
        LOG.debug("calculated spt in {}msec", (int)(t1-t0));

        Image image = vertexRaster.generateImage(spt);

        if (image != null) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                t0 = System.currentTimeMillis();
                ImageIO.write((BufferedImage) image, "png", out);
                final byte[] imgData = out.toByteArray();
                final InputStream bigInputStream = new ByteArrayInputStream(imgData);
                t1 = System.currentTimeMillis();
                LOG.debug("wrote png in {}msec", (int)(t1-t0));
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