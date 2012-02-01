package org.opentripplanner.analyst.rest;

import java.util.GregorianCalendar;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.geotools.geometry.Envelope2D;
import org.opentripplanner.analyst.core.SlippyTile;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SPTRequest;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.analyst.rest.parameter.Layer;
import org.opentripplanner.analyst.rest.parameter.Style;
import org.opentripplanner.analyst.rest.parameter.MIMEImageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.core.InjectParam;

@Path("tile/{z}/{x}/{y}.png")
@Component
@Scope("request")
public class TileService {
    
    private static final Logger LOG = LoggerFactory.getLogger(TileService.class);

    @InjectParam
    private Renderer renderer;

    @GET @Produces("image/png")
    public Response tileGet(
           @PathParam("x") int x, 
           @PathParam("y") int y, 
           @PathParam("z") int z,
           @QueryParam("lon")  Float originLon, 
           @QueryParam("lat")  Float originLat, 
           @QueryParam("time") GregorianCalendar time) 
           throws Exception { 
        
        // LOG.debug("params {}", uriInfo.getQueryParameters());
        // LOG.debug("srs is : {}", srs.getName());
        // LOG.debug("bbox is : {}", bbox);
        // LOG.debug("search time is : {}", time);
        
        if (originLat == null || originLon == null) {
            LOG.warn("no origin (sample dimension) specified.");
            return Response.noContent().build();
        }
        
        Envelope2D env = SlippyTile.tile2Envelope(x, y, z);
        TileRequest tileRequest = new TileRequest(env, 255, 255);
        SPTRequest sptRequest = new SPTRequest(originLon, originLat, time.getTimeInMillis()/1000);

        RenderRequest renderRequest = new RenderRequest(
                new MIMEImageFormat("image/png"), 
                Layer.TRAVELTIME,
                Style.GRAY,
                true);

        return renderer.getResponse(tileRequest, sptRequest, null, renderRequest);
    }

}