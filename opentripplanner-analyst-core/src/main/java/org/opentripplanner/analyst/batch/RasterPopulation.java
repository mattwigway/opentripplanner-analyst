package org.opentripplanner.analyst.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RasterPopulation extends Population {

    private static final Logger LOG = LoggerFactory.getLogger(RasterPopulation.class);
    public final int width, height;

    public RasterPopulation() {
        width = 0;
        height = 0;
    }
}
