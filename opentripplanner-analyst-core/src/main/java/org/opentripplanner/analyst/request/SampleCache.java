package org.opentripplanner.analyst.request;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.core.SampleSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SampleCache implements SampleSource {

    private final Sample emptySample = new Sample(null, 0, null, 0);
    private final Map<SampleRequest, Sample> cache =
            new ConcurrentHashMap<SampleRequest, Sample>();
    
    @Autowired
    private SampleFactory sampleFactory;
    
    // should distinguish between null sample and key not found
    
    @Override
    public Sample getSample(double lon, double lat) {
        SampleRequest sr = new SampleRequest(lon, lat);
        Sample ret = cache.get(sr);
        if (ret == null) {
            ret = sampleFactory.getSample(lon, lat);
            if (ret == null)
                ret = emptySample;
            cache.put(sr, ret);
        }
        return ret;
    }

}
