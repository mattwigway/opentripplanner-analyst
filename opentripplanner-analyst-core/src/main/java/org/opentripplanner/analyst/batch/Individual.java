package org.opentripplanner.analyst.batch;

import org.opentripplanner.routing.graph.Vertex;

/**
 * Individual locations that make up Populations for the purpose
 * of many-to-many searches.
 *  
 * @author andrewbyrd
 *
 */
public class Individual {
	public final double x, y;
	public double data;
	public double result;
	public Vertex vertex;
	
	public Individual(double x, double y, double data) {
		this.x = x;
		this.y = y;
		this.data = data;
		this.vertex = null;
		this.result = Double.NaN;
	}
}
