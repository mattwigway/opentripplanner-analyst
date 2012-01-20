package org.opentripplanner.analyst.core;

import java.util.Arrays;

import javax.media.jai.ImageFunction;

import org.opentripplanner.routing.spt.ShortestPathTree;

public class TravelTimeImageFunction implements ImageFunction {

    VertexRaster vr;
    ShortestPathTree spt;
    
    public TravelTimeImageFunction (VertexRaster vr, ShortestPathTree spt) {
        this.vr = vr;
        this.spt = spt;
    }
    
    @Override
    public void getElements(double startX, double startY, double deltaX, double deltaY, 
            int countX, int countY, int element, double[] real, double[] imag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getNumElements() {
        return 1;
    }

    @Override
    public boolean isComplex() {
        return false;
    }

    @Override
    public void getElements(float startX, float startY, float deltaX, float deltaY, 
            int countX, int countY, int element, float[] real, float[] imag) {
        int i = 0;
        Arrays.fill(real, Float.NaN);
        double lat = startY;
        for (int y=0; y<countY; lat+=deltaY, y+=1) {
            double lon = startX;
            for (int x=0; x<countX; lon+=deltaX, x+=1) {
                VertexRaster.Sample s = vr.makeSample(x, y, lon, lat);
                if (s != null) {
                    real[i] = (float) s.eval(spt);
                }
                i+=1;
            }
        }
    }
}
