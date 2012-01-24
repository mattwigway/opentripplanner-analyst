import static org.opentripplanner.common.IterableLibrary.filter;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.EndpointVertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.StreetVertex;
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
                Vertex in = new EndpointVertex("(" + x + ", " + y + ") in", xc, yc);
                graph.addVertex(in);
                verticesIn[y][x] = in;

                Vertex out = new EndpointVertex("(" + x + ", " + y + ") out", xc, yc);
                graph.addVertex(out);
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
                StreetVertex we = new StreetVertex("a(" + j + ", " + i + ")", geometry, "", d, false, null);
                StreetVertex ew = new StreetVertex("a(" + j + ", " + i + ")", (LineString) geometry.reverse(), "", d, true, null);
                
                graph.addVertex(we);
                graph.addVertex(ew);

                lon = i * STEP + LON0;
                lat = j * STEP + LAT0;
                d = 111.111;
                geometry = GeometryUtils.makeLineString(lon, lat, lon, lat + STEP);
                StreetVertex sn = new StreetVertex("d(" + i + ", " + j + ")", geometry, "", d, false, null);
                StreetVertex ns = new StreetVertex("d(" + i + ", " + j + ")", (LineString) geometry.reverse(), "", d, true, null);

                graph.addVertex(sn);
                graph.addVertex(ns);
                
                graph.addEdge(new FreeEdge(verticesOut[i][j], we));
                graph.addEdge(new FreeEdge(verticesOut[j][i], sn));
                
                graph.addEdge(new FreeEdge(verticesOut[i][j + 1], ew));
                graph.addEdge(new FreeEdge(verticesOut[j + 1][i], ns));
                
                graph.addEdge(new FreeEdge(ew, verticesIn[i][j]));
                graph.addEdge(new FreeEdge(ns, verticesIn[j][i]));
            
                graph.addEdge(new FreeEdge(we, verticesIn[i][j + 1]));
                graph.addEdge(new FreeEdge(sn, verticesIn[j + 1][i]));
            }
        }

        for (int y = 0; y < N; ++y) {
            for (int x = 0; x < N; ++x) {
                Vertex vertexIn = verticesIn[y][x];
                for (DirectEdge e1: filter(vertexIn.getIncoming(),DirectEdge.class)) {
                    Vertex vertexOut = verticesOut[y][x];
                    StreetVertex fromv = (StreetVertex) e1.getFromVertex();
                    for (DirectEdge e2: filter(vertexOut.getOutgoing(),DirectEdge.class)) {
                        StreetVertex tov = (StreetVertex) e2.getToVertex();
                        if (tov.getEdgeId().equals(fromv.getEdgeId())) {
                            continue;
                        }
                        graph.addEdge(new TurnEdge(fromv, tov));
                    }
                }
            }
        }
        return graph;
    }

}
