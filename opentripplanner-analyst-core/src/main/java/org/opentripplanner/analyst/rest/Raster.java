package org.opentripplanner.analyst.rest;

import java.util.GregorianCalendar;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.geotools.geometry.Envelope2D;
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
    
    @GET @Produces("image/*")
    public Response getRaster(
           @QueryParam("x") Float x,  
           @QueryParam("y") Float y,  
           @QueryParam("width")  Integer width,  
           @QueryParam("height") Integer height,  
           @QueryParam("resolution") Double resolution,  
           @QueryParam("time") GregorianCalendar time,
           @QueryParam("format") @DefaultValue("image/geotiff") MIMEImageFormat format,
           @QueryParam("crs") @DefaultValue("EPSG:4326") CoordinateReferenceSystem crs 
           ) throws Exception {
        
        // BoundingBox is a subclass of Envelope, an Envelope2D constructor parameter
        Envelope2D bbox = new Envelope2D(index.getBoundingBox(crs));
        if (resolution != null) {
            width  = (int) Math.ceil(bbox.width  / resolution);
            height = (int) Math.ceil(bbox.height / resolution);
        }
        TileRequest tileRequest = new TileRequest(bbox, width, height);
        SPTRequest sptRequest = new SPTRequest(x, y, time);
        RenderRequest renderRequest = new RenderRequest(format, Layer.TRAVELTIME, Style.GRAY, false);

        return renderer.getResponse(tileRequest, sptRequest, null, renderRequest);
        
    }

}