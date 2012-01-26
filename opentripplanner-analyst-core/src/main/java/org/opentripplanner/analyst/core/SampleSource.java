package org.opentripplanner.analyst.core;

public interface SampleSource {

    Sample getSample(int gx, int gy, double lon, double lat);

}
