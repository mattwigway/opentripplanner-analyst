package org.opentripplanner.analyst.rest.parameter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class MIMEImageFormat {

    public final String type;
            
    public MIMEImageFormat(String s) {
        String[] parts = s.split("/");
        if (parts.length == 2 && parts[0].equals("image")) {
            if (parts[1].equals("png") || parts[1].equals("gif") || parts[1].equals("jpeg") || parts[1].equals("geotiff")) {
                type = parts[1];
            } else {
                throw new WebApplicationException(Response
                        .status(Status.BAD_REQUEST)
                        .entity("unsupported image format: " + parts[1])
                        .build());
            }
        } else {
            throw new WebApplicationException(Response
                    .status(Status.BAD_REQUEST)
                    .entity("malformed image format mime type: " + s)
                    .build());
        }
    }
 
    public String toString() {
        return "image/" + type;
    }
}
