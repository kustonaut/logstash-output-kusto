package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.Output;
import co.elastic.logstash.api.PluginConfigSpec;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.*;

import static org.logstashplugins.KustoConfigProperties.*;

// class name must match plugin name
@LogstashPlugin(name = "kusto_output")
public class KustoOutput implements Output {
    private final Executor executor;
    private final String path;

    private final String id;
    private final CountDownLatch done = new CountDownLatch(1);
    private volatile boolean stopped = false;

    // all plugins must provide a constructor that accepts id, Configuration, and Context
    public KustoOutput(final String id, final Configuration configuration, final Context context) {
        int maxThreads = configuration.get(UPLOAD_CONCURRENT_COUNT).intValue();
        this.id = id;
        this.path = configuration.get(PATH_CONFIG);
        this.executor = new ThreadPoolExecutor(1,maxThreads,500, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
    }


    @Override
    public void output(final Collection<Event> events) {
        try {
            Iterator<Event> z = events.iterator();
            while (z.hasNext() && !stopped) {
                Event logEvent = z.next();
                logEvent.sprintf(this.path);
            }
        }catch (IOException ignored){

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
