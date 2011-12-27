package org.opentripplanner.analyst.core;

import java.util.Arrays;

import javax.media.jai.ImageFunction;

import org.opentripplanner.common.geometry.HashGrid;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import com.vividsolutions.jts.geom.Coordinate;

public class TravelTimeImageFunction implements ImageFunction {

    HashGrid<Vertex> hashGrid;
    ShortestPathTree spt;
    
    public TravelTimeImageFunction (HashGrid<Vertex> hashGrid, ShortestPathTree spt) {
        this.hashGrid = hashGrid;
        this.spt = spt;
    }
    
    @Override
    public void getElements(double startX, double startY, double deltaX, double deltaY, 
            int countX, int countY, int element, double[] real, double[] imag) {
        int i = 0;
        Arrays.fill(real, Double.NaN);
        for (double lon=startX, x=0; x<countX; lon+=deltaX, x+=1) {
            for (double lat=startY, y=0; y<countY; lat+=deltaY, y+=1) {
//                Vertex v = hashGrid.closest(lon, lat, 150);
//                if (v != null) {
//                    State s = spt.getState(v);
//                    if (s != null)
//                        real[i] = s.getElapsedTime();
//                }
                real[i] = i;
                i+=1;
            }
        }
    }

    @Override
    public int getNumElements() {
        return 1;
    }

    @Override
    public boolean isComplex() {
        return false;
    }

    // gridCoverageWriter is using this one
    @Override
    public void getElements(float startX, float startY, float deltaX, float deltaY, 
            int countX, int countY, int element, float[] real, float[] imag) {
        int i = 0;
        Arrays.fill(real, Float.NaN);
        for (double lat=startY, y=0; y<countY; lat+=deltaY, y+=1) {
        for (double lon=startX, x=0; x<countX; lon+=deltaX, x+=1) {
                Vertex v = hashGrid.closest(lon, lat, 200);
                if (v != null) {
                    State s = spt.getState(v);
                    if (s != null)
                        real[i] = (float) (s.getElapsedTime() + v.distance(new Coordinate(lon, lat)));
                }
                //real[i] = (i / (float)(countX * countY));
                i+=1;
            }
        }
    }
}
