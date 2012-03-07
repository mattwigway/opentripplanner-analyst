package org.opentripplanner.analyst;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TurnVertex;
import org.opentripplanner.routing.edgetype.TurnEdge;

import com.vividsolutions.jts.geom.LineString;

public class ArtificialGraphGenerator {

    public static Graph generateGridGraph() {
        final double LAT0 = 01.0;
        final double LON0 = 65.0;
        final double STEP = 0.001;
        // number of rows and columns in grid
        final int N = 100;

        Graph graph = new Graph();
        // create a NxN grid of vertices
        Vertex[][] verticesIn = new Vertex[N][];
        Vertex[][] verticesOut = new Vertex[N][];
        for (int y = 0; y < N; ++y) {
            verticesIn[y] = new Vertex[N];
            verticesOut[y] = new Vertex[N];
            for (int x = 0; x < N; ++x) {
                double xc = x * STEP + LON0;
                double yc = y * STEP + LAT0;
                Vertex in = new IntersectionVertex(graph, "(" + x + ", " + y + ") in", xc, yc);
                verticesIn[y][x] = in;

                Vertex out = new IntersectionVertex(graph, "(" + x + ", " + y + ") out", xc, yc);
                verticesOut[y][x] = out;
            }
        }


        /*
         * (i, j) iteration variables are used for: 
         * (y, x) and (lat, lon) respectively when making west-east streets
         * (x, y) and (lon, lat) respectively when making south-north streets
         * 
         * verticesOut are connected to all StreetVertex leading away from the given grid point
         * verticesIn are connected to all StreetVertex leading toward the given grid point
         * Note: this means that in a search, the last TurnEdge at the destination will not be traversed 
         */
        for (int i = 0; i < N; ++i) {
            for (int j = 0; j < N - 1; ++j) {
                double lon = j * STEP + LON0;
                double lat = i * STEP + LAT0;
                double d = 111.111;
                LineString geometry = GeometryUtils.makeLineString(lon, lat, lon + STEP, lat);
                TurnVertex we = new TurnVertex(graph, "a(" + j + ", " + i + ")", geometry, "", d, false, null);
                TurnVertex ew = new TurnVertex(graph, "a(" + j + ", " + i + ")", (LineString) geometry.reverse(), "", d, true, null);
                
                lon = i * STEP + LON0;
                lat = j * STEP + LAT0;
                d = 111.111;
                geometry = GeometryUtils.makeLineString(lon, lat, lon, lat + STEP);
                TurnVertex sn = new TurnVertex(graph, "d(" + i + ", " + j + ")", geometry, "", d, false, null);
                TurnVertex ns = new TurnVertex(graph, "d(" + i + ", " + j + ")", (LineString) geometry.reverse(), "", d, true, null);
               
                new FreeEdge(verticesOut[i][j], we);
                new FreeEdge(verticesOut[j][i], sn);
                
                new FreeEdge(verticesOut[i][j + 1], ew);
                new FreeEdge(verticesOut[j + 1][i], ns);
                
                new FreeEdge(ew, verticesIn[i][j]);
                new FreeEdge(ns, verticesIn[j][i]);
            
                new FreeEdge(we, verticesIn[i][j + 1]);
                new FreeEdge(sn, verticesIn[j + 1][i]);
            }
        }

        for (int y = 0; y < N; ++y) {
            for (int x = 0; x < N; ++x) {
                Vertex vertexIn = verticesIn[y][x];
                for (Edge e1: vertexIn.getIncoming()) {
                    Vertex vertexOut = verticesOut[y][x];
                    TurnVertex fromv = (TurnVertex) e1.getFromVertex();
                    for (Edge e2: vertexOut.getOutgoing()) {
                        TurnVertex tov = (TurnVertex) e2.getToVertex();
                        if (tov.getEdgeId().equals(fromv.getEdgeId())) {
                            continue;
                        }
                        new TurnEdge(fromv, tov);
                    }
                }
            }
        }
        return graph;
    }

}
