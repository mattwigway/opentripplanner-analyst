package org.opentripplanner.analyst.rest;
 
import java.io.InputStream;
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
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SPTRequest;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.analyst.rest.parameter.Layer;
import org.opentripplanner.analyst.rest.parameter.LayerList;
import org.opentripplanner.analyst.rest.parameter.MIMEImageFormat;
import org.opentripplanner.analyst.rest.parameter.Style;
import org.opentripplanner.analyst.rest.parameter.StyleList;
import org.opentripplanner.analyst.rest.parameter.WMSVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.core.InjectParam;

@Path("wms")
@Component
@Scope("request")
public class WebMapService {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebMapService.class);

    @InjectParam
    private Renderer renderer;
    
    @GET @Produces("image/*, text/xml")
    public Response wmsGet(
           // Mandatory parameters
           @QueryParam("version") WMSVersion version,
           @QueryParam("request") String request,
           @QueryParam("layers")  LayerList layers, 
           @QueryParam("styles")  StyleList styles,
           // called CRS in 1.3.0
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
           @QueryParam("DIM_TIMEB") GregorianCalendar timeB,
           @QueryParam("DIM_ORIGINLONB") Float originLonB, 
           @QueryParam("DIM_ORIGINLATB") Float originLatB,
           @Context UriInfo uriInfo ) throws Exception { 
        
        if (request.equals("getCapabilities")) 
            return getCapabilitiesResponse();
                    
        LOG.debug("params {}", uriInfo.getQueryParameters());
        LOG.debug("layers = {}", layers);
        LOG.debug("styles = {}", styles);
        LOG.debug("version = {}", version);
        LOG.debug("srs = {}", srs.getName());
        LOG.debug("bbox = {}", bbox);
        LOG.debug("search time = {}", time);
        if (originLat == null || originLon == null) {
            LOG.warn("no origin (sample dimension) specified.");
            return Response.noContent().build();
        }

        bbox.setCoordinateReferenceSystem(srs);
        TileRequest tileRequest = new TileRequest(bbox, width, height);
        SPTRequest  sptRequestA = new SPTRequest(originLon, originLat, time.getTimeInMillis()/1000);
        SPTRequest  sptRequestB = new SPTRequest(originLonB, originLatB, timeB.getTimeInMillis()/1000);
        
        Layer layer = layers.get(0);
        Style style = styles.get(0);
        RenderRequest renderRequest = new RenderRequest(format, layer, style, transparent);
        return renderer.getResponse(tileRequest, sptRequestA, sptRequestB, renderRequest);
    }

    /** Yes, this is loading a static capabilities response from a file 
     * on the classpath. */
    private Response getCapabilitiesResponse() throws Exception {
        InputStream xml = getClass().getResourceAsStream("wms-capabilities.xml");
        return Response.ok().entity(xml).type("text/xml").build();
    }
    
}