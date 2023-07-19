package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Event;
import org.junit.Assert;
import org.junit.Test;
import org.logstash.plugins.ConfigurationImpl;
import org.logstashplugins.KustoOutput;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class KustoOutputTest {

    @Test
    public void testKustoOutput() {
        String prefix = "Prefix";
        Map<String, Object> configValues = new HashMap<>();
        configValues.put(KustoOutput.PREFIX_CONFIG.name(), prefix);
        Configuration config = new ConfigurationImpl(configValues);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        KustoOutput output = new KustoOutput("test-id", config, null, baos);

        String sourceField = "message";
        int eventCount = 5;
        Collection<Event> events = new ArrayList<>();
        for (int k = 0; k < eventCount; k++) {
            Event e = new org.logstash.Event();
            e.setField(sourceField, "message " + k);
            events.add(e);
        }

        output.output(events);

        String outputString = baos.toString();
        int index = 0;
        int lastIndex = 0;
        while (index < eventCount) {
            lastIndex = outputString.indexOf(prefix, lastIndex);
            Assert.assertTrue("Prefix should exist in output string", lastIndex > -1);
            lastIndex = outputString.indexOf("message " + index);
            Assert.assertTrue("Message should exist in output string", lastIndex > -1);
            index++;
        }
    }
}
