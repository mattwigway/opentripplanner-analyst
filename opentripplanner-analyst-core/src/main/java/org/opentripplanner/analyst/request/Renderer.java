package org.opentripplanner.analyst.request;

import java.awt.image.BufferedImage;

import javax.ws.rs.core.Response;

import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.rest.utils.TileUtils;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Renderer {

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
            long elapsed = sptRequestB.time - sptRequestA.time;
            image = tile.generateImageHagerstrand(sptA, sptB, elapsed, renderRequest);
            break;
        case TRAVELTIME :
        default :
            image = tile.generateImage(sptA, renderRequest);
        }
        
        return TileUtils.generateStreamingImageResponse(image, renderRequest.format);
    }
    
}
