package org.opentripplanner.analyst.batch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * A collection of individual locations that will be used as either the origin set or the destination set in a many-to-many search.
 * 
 * @author andrewbyrd
 */
public class Population {
    
    private static final Logger LOG = LoggerFactory.getLogger(Population.class);
    
    public final List<Individual> individuals = new ArrayList<Individual>();

    public void writeCsv(String outFileName, ShortestPathTree spt) {
        LOG.debug("Writing population to CSV: {}", outFileName);
        File outFile = new File(outFileName);
        PrintWriter csvWriter;
        try {
            csvWriter = new PrintWriter(outFile);
            csvWriter.printf("data;traveltime\n");
            for (Individual i : individuals) {
                Sample s = i.sample;
                long t = Long.MAX_VALUE;
                if (s != null)
                    t = s.eval(spt);
                csvWriter.printf("%f;%d\n", i.data, t);
            }
            csvWriter.close();
        } catch (Exception e) {
            LOG.debug("error writing population to CSV: {}", e);
        }
        LOG.debug("Done writing population to CSV.");
    }
    
}
