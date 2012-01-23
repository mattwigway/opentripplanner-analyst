package org.opentripplanner.analyst.batch;

import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.analyst.core.Tile.Sample;

/**
 * Individual locations that make up Populations for the purpose
 * of many-to-many searches.
 *  
 * @author andrewbyrd
 *
 */
public class Individual {

    public final String id;
    public final Sample sample;
    public double data;
    
    public Individual(String id, Sample sample, double data) {
        this.id = id;
        this.sample = sample;
        this.data = data;
    }

    public Individual(String id, double lon, double lat, double data) {
        Sample sample = Tile.makeSample(0, 0, lon, lat);
        this.id = id;
        this.sample = sample;
        this.data = data;
    }
    
}
