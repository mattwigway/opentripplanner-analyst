package org.opentripplanner.analyst.rest.parameter;

import java.util.ArrayList;
import java.util.Arrays;

public class LayerList extends ArrayList<String> {

    public LayerList(String v) {
      super(Arrays.asList(v.split(",")));
    }

}

