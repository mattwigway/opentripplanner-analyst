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
    private TileCache tileFactory;

    @Autowired
    private SPTCache sptFactory;

    public Response getResponse (TileRequest tileRequest, 
            SPTRequest sptRequestA, SPTRequest sptRequestB, 
            RenderRequest renderRequest) throws Exception {

        Tile tile = tileFactory.get(tileRequest);
        ShortestPathTree sptA = sptFactory.get(sptRequestA);
        ShortestPathTree sptB = sptFactory.get(sptRequestB);
        
        BufferedImage image;
        switch (renderRequest.layer) {
        case DIFFERENCE :
            image = tile.generateImageDifference(sptA, sptB, renderRequest);
            break;
        case HAGERSTRAND :
            long elapsed = sptRequestB.time - sptRequestA.time;
            image = tile.generateImageHagerstrand(sptA, sptB, elapsed, renderRequest);
            break;
        default :
            image = tile.generateImage(sptA, renderRequest);
        }
        
        return TileUtils.generateImageResponse(image, renderRequest.format);
    }
    
}
