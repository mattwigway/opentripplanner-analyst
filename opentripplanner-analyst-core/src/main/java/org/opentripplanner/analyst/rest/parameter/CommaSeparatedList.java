package org.opentripplanner.analyst.rest.parameter;

import java.util.ArrayList;
import java.util.Arrays;

public class CommaSeparatedList extends ArrayList<String> {

    public CommaSeparatedList(String v) {
      super(Arrays.asList(v.split(",")));
    }

}

