package org.opentripplanner.analyst.rest;

import java.util.GregorianCalendar;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.opentripplanner.analyst.core.GeometryIndex;
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
    
    @GET @Produces("image/tiff")
    public Response getRaster(
           @QueryParam("lon") Float lon,  
           @QueryParam("lat") Float lat,  
           @QueryParam("time") GregorianCalendar time ) {
        
        //com.vividsolutions.jts.geom.Envelope graphEnvelope = graph.getExtent();
        return Response.noContent().build();
    }

}