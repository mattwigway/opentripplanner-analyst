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
import org.opengis.geometry.DirectPosition;
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
           @QueryParam("bbox") Envelope2D bbox,
           @QueryParam("width")  Integer width,  
           @QueryParam("height") Integer height,  
           @QueryParam("time") GregorianCalendar time,
           @QueryParam("crs") // default is WGS84 lat/lon
           @DefaultValue("EPSG:4326") CoordinateReferenceSystem crs ) throws Exception {
        
        LOG.debug("serving raster request!");
        //com.vividsolutions.jts.geom.Envelope graphEnvelope = graph.getExtent();

        LOG.debug("crs = {}", crs.getName());
        LOG.debug("bbox = {}", bbox);
        LOG.debug("search time = {}", time);

        // allow height to be left off, figure using constant Cartesian scale
        // assume height is to y and width is to x
        if (height == null) {
            DirectPosition upperCorner = bbox.getUpperCorner();
            DirectPosition lowerCorner = bbox.getLowerCorner();
            double resolution = (upperCorner.getOrdinate(0) - lowerCorner.getOrdinate(0))/width;
            LOG.debug("resolution for request: " + resolution);
            height = (int) Math.round((
                upperCorner.getOrdinate(1) - lowerCorner.getOrdinate(1)) * resolution);
        }

        LOG.debug("calculated height: " + height);

        bbox.setCoordinateReferenceSystem(crs);

        TileRequest tileRequest = new TileRequest(bbox, width, height);
        LOG.debug("Tile");
        SPTRequest sptRequest = new SPTRequest(x, y, time.getTimeInMillis()/1000);
        LOG.debug("SPT");
        //  format (GeoTIFF), travel time layer, gray style, not transparent
        RenderRequest renderRequest = new RenderRequest(
                new MIMEImageFormat("image/geotiff"), 
                Layer.TRAVELTIME, Style.GRAY, false);

        LOG.debug("got this far!");

        // no second SPT, not a difference request
        return renderer.getResponse(tileRequest, sptRequest, null, renderRequest);
        
    }

}