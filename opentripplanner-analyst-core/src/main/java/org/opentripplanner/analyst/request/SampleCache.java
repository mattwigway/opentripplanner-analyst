package org.opentripplanner.analyst.request;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.core.SampleSource;

public class SampleCache implements SampleSource {

    private Sample emptySample = new Sample(0, 0, null, 0, null, 0);
    private SampleSource source;
    private Map<SampleRequest, Sample> cache;
    
    public SampleCache (SampleSource source) {
        this.source = source;
        this.cache = new ConcurrentHashMap<SampleRequest, Sample>();
    }
    
    // must distinguish between null sample and key not found
    
    @Override
    public Sample getSample(int gx, int gy, double lon, double lat) {
        SampleRequest sr = new SampleRequest(lon, lat);
        Sample ret = cache.get(sr);
        if (ret == null) {
            //System.out.printf("cache miss %d %d\n", sr.lon, sr.lat);
            ret = source.getSample(gx, gy, lon, lat);
            if (ret == null)
                ret = emptySample;
            cache.put(sr, ret);
        } else {
            //System.out.printf("cache hit  %d %d\n", sr.lon, sr.lat);
        }
        return ret;
    }

}
