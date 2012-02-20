package org.opentripplanner.analyst.request;

import java.awt.image.BufferedImage;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.rest.parameter.MIMEImageFormat;
import org.opentripplanner.analyst.rest.utils.TileUtils;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Renderer {

    private static final Logger LOG = LoggerFactory.getLogger(Renderer.class);

    @Autowired
    private TileCache tileCache;

    @Autowired
    private SPTCache sptCache;

    public Response getResponse (TileRequest tileRequest, 
            SPTRequest sptRequestA, SPTRequest sptRequestB, 
            RenderRequest renderRequest) throws Exception {

        Tile tile = tileCache.get(tileRequest);
        ShortestPathTree sptA = sptCache.get(sptRequestA);
        ShortestPathTree sptB = sptCache.get(sptRequestB);
        
        BufferedImage image;
        switch (renderRequest.layer) {
        case DIFFERENCE :
            image = tile.generateImageDifference(sptA, sptB, renderRequest);
            break;
        case HAGERSTRAND :
            long elapsed = Math.abs(sptRequestB.time - sptRequestA.time);
            image = tile.generateImageHagerstrand(sptA, sptB, elapsed, renderRequest);
            break;
        case TRAVELTIME :
        default :
            image = tile.generateImage(sptA, renderRequest);
        }
        
        // TODO: geotiff from gridcoverage
        //GridCoverage2D gc = tile.getGridCoverage2D(image);
        return generateStreamingImageResponse(image, renderRequest.format);
    }
    
    private static Response generateStreamingImageResponse(
            final BufferedImage image, final MIMEImageFormat format) {
        
        if (image == null) {
            LOG.warn("response image is null");
        }
            
        StreamingOutput streamingOutput = new StreamingOutput() {
            public void write(OutputStream outStream) {
                try {
                    long t0 = System.currentTimeMillis();
                    ImageIO.write(image, format.type, outStream);
                    long t1 = System.currentTimeMillis();
                    LOG.debug("wrote image in {}msec", (int)(t1-t0));
                } catch (Exception e) {
                    LOG.error("exception while preparing image : {}", e.getMessage());
                    throw new WebApplicationException(e);
                }
            }
       };

       CacheControl cc = new CacheControl();
       cc.setMaxAge(3600);
       cc.setNoCache(false);
       return Response.ok(streamingOutput)
                       .type(format.toString())
                       .cacheControl(cc)
                       .build();
    }

}
