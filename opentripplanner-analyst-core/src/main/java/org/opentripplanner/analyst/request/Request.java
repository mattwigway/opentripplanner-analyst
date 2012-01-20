package org.opentripplanner.analyst.request;

/**
 * 
 * @author andrewbyrd
 *
 * @param <T>
 */
public interface Request<T> {

    public abstract T getResponse();
    
    public abstract T buildResponse();
    
}
