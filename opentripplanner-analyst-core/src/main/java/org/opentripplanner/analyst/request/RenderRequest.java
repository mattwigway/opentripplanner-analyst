package org.opentripplanner.analyst.request;

import org.geotools.geometry.Envelope2D;
import org.opentripplanner.analyst.rest.parameter.Style;
import org.opentripplanner.analyst.rest.parameter.MIMEImageFormat;

public class RenderRequest {

    public final MIMEImageFormat format; 
    public final Style style; 
    public final boolean transparent;

    public RenderRequest (MIMEImageFormat format, Style style, boolean transparent) {
        this.format = format;
        this.style = style;
        this.transparent = transparent;
    }
    
    public String toString() {
        return String.format("<render request format=%s style=%s", 
                format, style);
    }

}
