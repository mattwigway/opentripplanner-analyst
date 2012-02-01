package org.opentripplanner.analyst.rest;

import java.util.GregorianCalendar;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.analyst.core.GeometryIndex;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SPTRequest;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.analyst.rest.parameter.Layer;
import org.opentripplanner.analyst.rest.parameter.MIMEImageFormat;
import org.opentripplanner.analyst.rest.parameter.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.core.InjectParam;

@Path("raster")
@Component
@Scope("request")
public class Raster {
    
    private static final Logger LOG = LoggerFactory.getLogger(Raster.class);

    @InjectParam
    private GeometryIndex index;

    @InjectParam
    private Renderer renderer;

    @GET @Produces("image/geotiff")
    public Response getRaster(
           @QueryParam("x") Float x,  
           @QueryParam("y") Float y,  
           @QueryParam("width")  Float width,  
           @QueryParam("height") Float height,  
           @QueryParam("time") GregorianCalendar time,
           @QueryParam("crs") // default is WGS84 lat/lon
           @DefaultValue("EPSG:4326") CoordinateReferenceSystem crs ) throws Exception {
        
        //com.vividsolutions.jts.geom.Envelope graphEnvelope = graph.getExtent();

        Envelope2D bbox = index.getBoundingEnvelope(srs);
        TileRequest tileRequest = new TileRequest(bbox, width, height);
        SPTRequest sptRequest = new SPTRequest(x, y, time);
        RenderRequest renderRequest = new RenderRequest(
                new MIMEImageFormat("text/geotiff"), 
                Layer.TRAVELTIME, Style.GRAY, false);

        return renderer.getResponse(tileRequest, sptRequest, null, renderRequest);
        
    }

}