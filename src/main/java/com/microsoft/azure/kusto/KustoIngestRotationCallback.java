package com.microsoft.azure.kusto;

import com.microsoft.azure.kusto.data.auth.ConnectionStringBuilder;
import com.microsoft.azure.kusto.ingest.IngestClient;
import com.microsoft.azure.kusto.ingest.IngestClientFactory;
import com.microsoft.azure.kusto.ingest.source.StreamSourceInfo;
import com.vlkan.rfos.RotationCallback;
import com.vlkan.rfos.policy.RotationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;

/**
 * A container singleton class that provides access to the platform
 * dependent objects Platform, Options, and Crawler. This singleton
 * must be build using the provider Builder and should only be built
 * once.
 */
public class KustoIngestRotationCallback implements RotationCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(KustoIngestRotationCallback.class);
    private static KustoIngestRotationCallback instance = null;
    private final IngestClient ingestClient;

    private KustoIngestRotationCallback(Builder builder) {
        try {
            ConnectionStringBuilder kcsb = null;
            if (builder.isManagedIdentity) {
                if (builder.isSystemManagedIdentity) {
                    kcsb = ConnectionStringBuilder.createWithAadManagedIdentity(builder.ingestUrl);
                } else {
                    kcsb = ConnectionStringBuilder.createWithAadManagedIdentity(builder.ingestUrl, builder.managedIdentityId);
                }
            } else {
                kcsb = ConnectionStringBuilder.createWithAadApplicationCredentials(builder.ingestUrl, builder.appId, builder.appKey, builder.tenantId);
            }
            this.ingestClient = IngestClientFactory.createClient(kcsb);
            instance = this;
        } catch (Exception initException) {
            throw new RuntimeException("Failed to initialize KustoIngestRotationCallback", initException);
        }
    }

    public static KustoIngestRotationCallback instance() {
        return instance;
    }

    public static Builder newBuilder() {
        if (instance != null) {
            throw new IllegalStateException("Policy has already been built.");
        }
        return new Builder();
    }

    public static IngestClient getIngestClient() {
        return instance().ingestClient;
    }

    public static KustoIngestRotationCallback getInstance() {
        return instance();
    }

    @Override
    public void onTrigger(RotationPolicy policy, Instant instant) {
        LOGGER.debug("Opening file {policy={}, instant={}}", policy, instant);
    }

    @Override
    public void onOpen(RotationPolicy policy, Instant instant, OutputStream ignored) {
        LOGGER.debug("file open {policy={}, instant={}}", policy, instant);
    }

    @Override
    public void onClose(RotationPolicy policy, Instant instant, OutputStream stream) {
        LOGGER.debug("Closing file {policy={}, instant={}}", policy, instant);
    }

    @Override
    public void onSuccess(RotationPolicy policy, Instant instant, File file) {
        UUID sourceId = UUID.randomUUID();
        LOGGER.info("Ingesting file {policy={}, instant={}, file={}} with source id {}", policy, instant, file.getName(), sourceId);
        try (InputStream fileStream = Files.newInputStream(file.toPath())) {
            StreamSourceInfo streamSourceInfo = new StreamSourceInfo(fileStream, false, sourceId);
            ingestClient.ingestFromStream(streamSourceInfo, null);
        } catch (Exception e) {
            LOGGER.error("Failed to ingest file", e);
        }
    }

    @Override
    public void onFailure(RotationPolicy policy, Instant instant, File file, Exception error) {
        String message = String.format("rotation failure {policy=%s, instant=%s, file=%s}", policy, instant, file);
        LOGGER.error(message, error);
    }

    /**
     * This method clears singleton and should only be called from
     * instrumented and unit test scripts.
     */
    protected void destroy() {
        instance = null;
    }

    /**
     * {@code Device} builder static inner class.
     */
    public static final class Builder {
        private String ingestUrl;
        private String appId;
        private String appKey;
        private String tenantId;
        private String managedIdentityId;
        private boolean isManagedIdentity = false;
        private boolean isSystemManagedIdentity = false;

        private Builder() {
        }


        public Builder ingestUrl(String val) {
            ingestUrl = val;
            return this;
        }

        public Builder appId(String val) {
            appId = val;
            return this;
        }

        public Builder appKey(String val) {
            appKey = val;
            return this;
        }

        public Builder tenantId(String val) {
            tenantId = val;
            return this;
        }

        public Builder managedIdentityId(String val) {
            managedIdentityId = val;
            isManagedIdentity = true;
            isSystemManagedIdentity = "system".equalsIgnoreCase(val);
            return this;
        }

        public KustoIngestRotationCallback build() {
            return new KustoIngestRotationCallback(this);
        }
    }
}