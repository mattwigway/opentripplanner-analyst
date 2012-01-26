package org.opentripplanner.analyst.batch;

import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.core.Sample;

/**
 * Individual locations that make up Populations for the purpose
 * of many-to-many searches.
 *  
 * @author andrewbyrd
 *
 */
public class Individual {

    public final Object id;
    public final Sample sample;
    public final double data;
    public final double lon, lat;
    
    public Individual(Object id, double lon, double lat, double data) {
        // TODO: makesample is no longer static, move to 
        Sample sample = null; //Tile.makeSample(0, 0, lon, lat);
        this.id = id;
        this.sample = sample;
        this.data = data;
        this.lon = lon;
        this.lat = lat;
    }
    
}
