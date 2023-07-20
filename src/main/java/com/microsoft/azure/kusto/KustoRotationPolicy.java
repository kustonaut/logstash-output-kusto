package com.microsoft.azure.kusto;

import com.vlkan.rfos.Clock;
import com.vlkan.rfos.policy.TimeBasedRotationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class KustoRotationPolicy extends TimeBasedRotationPolicy {
    private static final Logger LOGGER = LoggerFactory.getLogger(KustoRotationPolicy.class);

    private final int rotateSeconds;

    KustoRotationPolicy(int rotateSeconds) {
        this.rotateSeconds = rotateSeconds;
    }

    /**
     * @return the instant of the upcoming next trigger
     */
    @Override
    public Instant getTriggerInstant(Clock clock) {
        return clock.now().plusSeconds(rotateSeconds);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public String toString() {
        return "KustoRotationPolicy";
    }
}
