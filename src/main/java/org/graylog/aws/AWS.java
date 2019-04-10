package org.graylog.aws;

import com.amazonaws.regions.Regions;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * A common utils class for the AWS plugin.
 */
public class AWS {

    public static final String SOURCE_GROUP_IDENTIFIER = "aws_source";
    public static final String FIELD_LOG_GROUP = "aws_log_group";
    public static final String FIELD_LOG_STREAM = "aws_log_stream";

    // This is a non-instantiable utils class.
    private AWS() {
    }

    /**
     * Build a list of region choices with both a value (persisted in configuration) and display value (shown to the user).
     *
     * The display value is formatted nicely: "EU (London): eu-west-2"
     * The value is eventually passed to Regions.fromName() to get the actual region object: eu-west-2
     * @return a choices map with configuration value map keys and display value map values.
     */
    public static Map<String, String> buildRegionChoices() {
        Map<String, String> regions = Maps.newHashMap();
        for (Regions region : Regions.values()) {

            String displayValue = String.format("%s: %s", region.getDescription(), region.getName());
            regions.put(region.getName(), displayValue);
        }
        return regions;
    }
}
