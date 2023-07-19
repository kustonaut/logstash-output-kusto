package org.logstashplugins;

import co.elastic.logstash.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.logstashplugins.KustoConfigProperties.*;

// class name must match plugin name
@LogstashPlugin(name = "kusto_output")
public class KustoOutput implements Output {
    private static final Pattern FIELD_REF = Pattern.compile("/%\\{[^}]+\\}/");
    private static final Logger LOGGER = LogManager.getLogger(KustoOutput.class);
    private final Executor executor;
    private final String fileRoot;
    private final String path;
    private final String failurePath;
    //https://stackoverflow.com/questions/58935297/how-to-store-objects-in-memory-and-flush-them-to-a-destination-after-maxamount
    private final String id;
    private final CountDownLatch done = new CountDownLatch(1);
    private volatile boolean stopped = false;

    // all plugins must provide a constructor that accepts id, Configuration, and Context
    public KustoOutput(final String id, final Configuration configuration, final Context context) {
        int maxThreads = configuration.get(UPLOAD_CONCURRENT_COUNT).intValue();
        this.id = id;
        this.path = configuration.get(PATH_CONFIG);
        this.failurePath = configuration.get(FILENAME_FAILURE);
        this.fileRoot = Arrays.stream(this.path.split(System.lineSeparator())).sequential().filter(pathPart -> FIELD_REF.matcher(pathPart).find()).collect(Collectors.joining(System.lineSeparator()));
        this.executor = new ThreadPoolExecutor(1, maxThreads, 500, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public void output(final Collection<Event> events) {
        try {
            Iterator<Event> z = events.iterator();
            while (z.hasNext() && !stopped) {
                Event logEvent = z.next();
                String fileOutputPath = logEvent.sprintf(this.path);
                if (FIELD_REF.matcher(fileOutputPath).find() && !fileOutputPath.startsWith(fileRoot)) {
                    // scenario where the path is dynamic and this falls outside the temp dir
                    LOGGER.warn("The event tried to write outside the files root, writing the event to the failure file. Event {} , FileName {} and FailurePath {} ", logEvent, fileOutputPath, failurePath);
                    fileOutputPath = failurePath;
                }
            }
        } catch (IOException ignored) {

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
        return getAllConfigs();
    }

    @Override
    public String getId() {
        return id;
    }
}
