package org.opentripplanner.analyst.batch.factory;

import java.io.File;
import java.util.Iterator;

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
import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.Population;
import org.opentripplanner.analyst.core.GeometryIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

@Component
public class PopulationFactory {

    @Autowired
    GeometryIndex index;

    @Autowired
    IndividualFactory individualFactory;
    
    public static Population fromCSV(String filename) {
        Population ret = new Population();
        // Individual i = new Individual();
        // ret.add(i);
        return ret;
    }

    public static Population fromShapefile(
            String filename, 
            String idAttribute, 
            String dataAttribute) {
        Population population = new Population();
        LOG.debug("Loading population from shapefile {}", filename);
        LOG.debug("Feature attributes: id in {}, data in {}", idAttribute, dataAttribute);
        try {
            File file = new File(filename);
            FileDataStore store = FileDataStoreFinder.getDataStore(file);
            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = store
                    .getFeatureSource();

            CoordinateReferenceSystem sourceCRS = featureSource.getInfo().getCRS();
            Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG",
                    hints);
            CoordinateReferenceSystem worldCRS = factory.createCoordinateReferenceSystem("EPSG:4326");

            DefaultQuery query = new DefaultQuery();
            query.setCoordinateSystem(sourceCRS);
            query.setCoordinateSystemReproject(worldCRS);

            FeatureCollection<SimpleFeatureType, SimpleFeature> features = featureSource
                    .getFeatures(query);
            Iterator<SimpleFeature> it = features.iterator();
            int i = 0;
            int f = 0;
            while (it.hasNext()) {
                SimpleFeature feature = it.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                Point point = null;
                if (geom instanceof Point) {
                    point = (Point) geom;
                } else if (geom instanceof Polygon) {
                    point = ((Polygon) geom).getCentroid();
                } else {
                    throw new IllegalStateException("Shapefile must contain either points or polygons.");
                }
                Object id = feature.getAttribute(idAttribute);
                double data = (Double) feature.getAttribute(dataAttribute);
                Individual individual = new Individual(
                        id, point.getX(), point.getY(), data);
                population.individuals.add(individual);
                i++;
                if (individual.sample != null)
                    f++;
            }
            LOG.debug("found vertices for {} features out of {}", f, i);
            features.close(it);
        } catch (Exception ex) {
            throw new IllegalStateException("Error loading population from shapefile ", ex);
        }
        LOG.debug("Done loading shapefile.");
        return population;
    }


}
