package org.graylog.aws;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;


public class AWSTest {

    // Verify that region choices are correctly built and formatted.
    @Test
    public void testRegionChoices() {

        Map<String, String> regionChoices = AWS.buildRegionChoices();

        // Check format of random region.
        assertTrue(regionChoices.containsValue("EU (London): eu-west-2"));
        assertTrue(regionChoices.containsKey("eu-west-2"));

        // Verify that the Gov regions are present.
        //"us-gov-west-1" -> "AWS GovCloud (US): us-gov-west-1"
        assertTrue(regionChoices.containsValue("AWS GovCloud (US): us-gov-west-1"));
        assertTrue(regionChoices.containsKey("us-gov-west-1"));
        assertTrue(regionChoices.containsValue("AWS GovCloud (US-East): us-gov-east-1"));
        assertTrue(regionChoices.containsKey("us-gov-east-1"));
    }
}