package org.opentripplanner.analyst.core;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class Sample {
    public final int t0, t1;
    public final Vertex v0, v1;
    
    public Sample (Vertex v0, int t0, Vertex v1, int t1) {
        this.v0 = v0;
        this.t0 = t0;
        this.v1 = v1;
        this.t1 = t1;
    }
    
    public byte evalByte(ShortestPathTree spt) {
        long t = eval(spt) / 60;
        if (t >= 255)
            t = 255;
        return (byte) t;
    }
    
    public long eval(ShortestPathTree spt) {
        State s0 = spt.getState(v0);
        State s1 = spt.getState(v1);
        long m0 = Long.MAX_VALUE;
        long m1 = Long.MAX_VALUE;
        if (s0 != null)
            m0 = (s0.getElapsedTime() + t0); 
        if (s1 != null)
            m1 = (s1.getElapsedTime() + t1); 
        return (m0 < m1) ? m0 : m1; 
    }
    
    public String toString() {
        return String.format("Sample: %s in %d min or %s in %d min\n", v0, t0, v1, t1);
    }
    
}

