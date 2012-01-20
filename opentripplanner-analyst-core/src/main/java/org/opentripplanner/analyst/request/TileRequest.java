package org.opentripplanner.analyst.request;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.QueryParam;

import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.analyst.core.Tile;

public class TileRequest implements Request<Tile> {

    private static final Map<TileRequest, Tile> cache = 
            new HashMap<TileRequest, Tile>();
    
    private final Envelope2D bbox; 
    private final int width; 
    private final int height; 

    public TileRequest(Envelope2D bbox, Integer width, Integer height) {
        this.bbox = bbox;
        this.width = width;
        this.height = height;
    }
    
    @Override
    public Tile getResponse() {
        Tile response = cache.get(this);
        if (response == null) {
            response = buildResponse();
            cache.put(this, response);
        }
        return response;
    }

    @Override
    public Tile buildResponse() {
        return null; //new Tile(this);
    }

    public int hashCode() {
            return bbox.hashCode() * 42677 +
                    width  * 32 +
                    height * 1307;
    }
    
    public boolean equals(Object other) {
        if (other instanceof TileRequest) {
            TileRequest that = (TileRequest) other;
            return this.bbox.equals(that.bbox) &&
                    this.width == that.width    &&
                    this.height == that.height;
        }
        return false;
    }
    
}
