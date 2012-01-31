package org.opentripplanner.analyst.rest.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.rest.parameter.MIMEImageFormat;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TileUtils.class);
    
    public static Response generateImageResponse(
            BufferedImage image, MIMEImageFormat format) {
        if (image == null) {
            LOG.warn("response image is null");
            return Response.noContent().build();
        }
            
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            long t0 = System.currentTimeMillis();
            ImageIO.write(image, format.type, out);
            final byte[] imgData = out.toByteArray();
            final InputStream bigInputStream = new ByteArrayInputStream(imgData);
            long t1 = System.currentTimeMillis();
            LOG.debug("wrote image in {}msec", (int)(t1-t0));
            ResponseBuilder rb = Response.ok(bigInputStream);
            CacheControl cc = new CacheControl();
            cc.setMaxAge(3600);
            cc.setNoCache(false);
            return rb.cacheControl(cc).build();
        } catch (final IOException e) {
            LOG.error("exception while preparing image : {}", e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
