package org.opentripplanner.analyst.core;

import org.opentripplanner.routing.graph.Vertex;

/**
 * Individual locations that make up Populations for the purpose
 * of many-to-many searches.
 *  
 * @author andrewbyrd
 *
 */
public class Individual {
	double x, y;
	double data;
	double result;
	Vertex vertex;
	
	public Individual(double x, double y, double data) {
		this.x = x;
		this.y = y;
		this.data = data;
		this.vertex = null;
		this.result = Double.NaN;
	}
}
