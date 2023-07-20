package com.microsoft.azure.kusto;

import co.elastic.logstash.api.*;
import com.vlkan.rfos.RotationConfig;
import com.vlkan.rfos.policy.SizeBasedRotationPolicy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// class name must match plugin name
@LogstashPlugin(name = "kusto")
public class KustoOutput implements Output {
    private static final Pattern FIELD_REF = Pattern.compile("/%\\{[^}]+\\}/");
    private static final Logger LOGGER = LogManager.getLogger(KustoOutput.class);
    private final Executor executor;
    private final String fileRoot;
    private final Boolean createIfDeleted;
    private final String path;
    private final String failurePath;
    //https://stackoverflow.com/questions/58935297/how-to-store-objects-in-memory-and-flush-them-to-a-destination-after-maxamount
    private final String id;
    private final CountDownLatch done = new CountDownLatch(1);
    private final String database;
    private final String table;
    private final RotationConfig config;
    private volatile boolean stopped = false;

    // all plugins must provide a constructor that accepts id, Configuration, and Context
    public KustoOutput(final String id, final Configuration configuration, final Context context) {
        int maxThreads = configuration.get(KustoConfigProperties.UPLOAD_CONCURRENT_COUNT).intValue();
        this.id = id;
        this.path = configuration.get(KustoConfigProperties.PATH_CONFIG);
        this.failurePath = configuration.get(KustoConfigProperties.FILENAME_FAILURE);
        this.createIfDeleted = configuration.get(KustoConfigProperties.CREATE_IF_DELETED);
        this.fileRoot = Arrays.stream(this.path.split(System.lineSeparator())).sequential().
                filter(pathPart -> FIELD_REF.matcher(pathPart).find()).collect(Collectors.joining(System.lineSeparator()));
        this.executor = new ThreadPoolExecutor(1, maxThreads, 500, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
        this.database = configuration.get(KustoConfigProperties.DATABASE);
        this.table = configuration.get(KustoConfigProperties.TABLE);
        long longFileSizeMb = configuration.get(KustoConfigProperties.INGEST_FILE_SIZE_MB);
        int secondsBeforeIngest = configuration.get(KustoConfigProperties.INGEST_TIME_SECONDS).intValue();
        String targetPath = String.format("%s.%s.%s", this.path, this.database, this.table);
        this.config = RotationConfig
                .builder()
                .filePattern(targetPath)
                .policy(new SizeBasedRotationPolicy(1024 * 1024 * longFileSizeMb /* longFileSize MiB */))
                .compress(true)
                .policy(new KustoRotationPolicy(secondsBeforeIngest))
                .build();
    }

    @Override
    public void output(final @NotNull Collection<Event> events) {
        Iterator<Event> z = events.iterator();
        while (z.hasNext() && !stopped) {
            Event logEvent = z.next();
            String fileOutputPath = getEventPath(logEvent);
        }
    }

    private String getEventPath(@NotNull Event logEvent) {
        try {
            String fileOutputPath = logEvent.sprintf(this.path);
            if (FIELD_REF.matcher(fileOutputPath).find() && !fileOutputPath.startsWith(fileRoot)) {
                // scenario where the path is dynamic and this falls outside the temp dir
                LOGGER.warn("The event tried to write outside the files root, writing the event to the " +
                        "failure file. Event {} , FileName {} and FailurePath {} ", logEvent, fileOutputPath, failurePath);
                return failurePath;
            }
            if (!this.createIfDeleted && Files.notExists(Paths.get(fileOutputPath))) {
                // The file is deleted and the config is to not create the file
                LOGGER.warn("The event tried to write to a file that does not exist and createIfDeleted does not exist " +
                        "Event {} , FileName {} and FailurePath {} ", logEvent, fileOutputPath, failurePath);
                return failurePath;
            }
            LOGGER.debug("Writing event to tmp file {}", fileOutputPath);
            return fileOutputPath;
        } catch (IOException ioException) {
            LOGGER.error("The event tried to write to a file and encountered an exception" +
                    "Event {} , FailurePath {}. IOException ", logEvent, failurePath, ioException);
            return failurePath;
        }
    }

    @Override
    public void stop() {
        stopped = true;
        done.countDown();
    }

    @Override
    public void awaitStop() throws InterruptedException {
        done.await();
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        return KustoConfigProperties.getAllConfigs();
    }

    @Override
    public String getId() {
        return id;
    }
}
