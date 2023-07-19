package org.logstashplugins;

import co.elastic.logstash.api.PluginConfigSpec;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KustoConfigProperties {

    /*
  # The path to the file to write. Event fields can be used here,
  # like `/var/log/logstash/%{host}/%{application}`
  # One may also utilize the path option for date-based log
  # rotation via the joda time format. This will use the event
  # timestamp.
  # E.g.: `path => "./test-%{+YYYY-MM-dd}.txt"` to create
  # `./test-2013-05-29.txt`
  #
  # If you use an absolute path you cannot start with a dynamic string.
  # E.g: `/%{myfield}/`, `/test-%{myfield}/` are not valid paths
 */
    public static final PluginConfigSpec<String> PATH_CONFIG =
            PluginConfigSpec.requiredStringSetting("path");

    /*
      # Flush interval (in seconds) for flushing writes to files.
      # 0 will flush on every message. Increase this value to recude IO calls but keep
      # in mind that events buffered before flush can be lost in case of abrupt failure.
     */
    public static final PluginConfigSpec<Long> FLUSH_INTERVAL =
            PluginConfigSpec.numSetting("flush_interval", 2);

    /*
      # If the generated path is invalid, the events will be saved
      # into this file and inside the defined path.
     */
    public static final PluginConfigSpec<String> FILENAME_FAILURE =
            PluginConfigSpec.stringSetting("filename_failure", "_filepath_failures");


    /*
      # Should the plugin recover from failure?
      #
      # If `true`, the plugin will look for temp files from past runs within the
      # path (before any dynamic pattern is added) and try to process them
      #
      # If `false`, the plugin will disregard temp files found
      config :recovery, validate: :boolean, default: true
     */
    public static final PluginConfigSpec<Boolean> RECOVERY = PluginConfigSpec.booleanSetting("recovery", true);
    /*
      # The Kusto endpoint for ingestion related communication. You can see it on the Azure Portal.
     */
    public static final PluginConfigSpec<String> INGEST_URL =  PluginConfigSpec.requiredStringSetting("ingest_url");

    /*  # The following are the credentails used to connect to the Kusto service */
    // # application id
    public static final PluginConfigSpec<String> APP_ID = PluginConfigSpec.stringSetting("app_id");
    //  # application key (secret)
    public static final PluginConfigSpec<String> APP_KEY = PluginConfigSpec.stringSetting("app_key");
    //  # aad tenant id
    public static final PluginConfigSpec<String> TENANT_ID = PluginConfigSpec.stringSetting("app_tenant");
    //  # managed identity id , system or user managed identity GUID
    public static final PluginConfigSpec<String> MANAGED_IDENTITY_ID = PluginConfigSpec.stringSetting("managed_identity_id");
    /*
      # The following are the data settings that impact where events are written to
     */
    //  # Database name
    public static final PluginConfigSpec<String> DATABASE = PluginConfigSpec.requiredStringSetting("database");
    //  # Target table name
    public static final PluginConfigSpec<String> TABLE = PluginConfigSpec.requiredStringSetting("table");
    public static final PluginConfigSpec<String> JSON_MAPPING = PluginConfigSpec.requiredStringSetting("json_mapping");

    /*
      # Host of the proxy , is an optional field. Can connect directly
    */
    public static final PluginConfigSpec<String> PROXY_HOST = PluginConfigSpec.stringSetting("proxy_host");
    /*
      # Port where the proxy runs , defaults to 80. Usually a value like 3128
    */
    public static final PluginConfigSpec<Long> PROXY_PORT = PluginConfigSpec.numSetting("proxy_port");
    /*
      # Check Proxy URL can be over http or https. Do we need it this way or ignore this & remove this
    */
    public static final PluginConfigSpec<String> PROXY_PROTOCOL = PluginConfigSpec.stringSetting("proxy_protocol","http");

    /*
    # Specify how many files can be kept in the upload queue before the main process starts processing them in the main thread (not healthy)
    */
    public static final PluginConfigSpec<Long> UPLOAD_QUEUE_SIZE = PluginConfigSpec.numSetting("upload_queue_size",30);

    /*
      # Specify how many files can be uploaded concurrently
     */
    public static final PluginConfigSpec<Long> UPLOAD_CONCURRENT_COUNT = PluginConfigSpec.numSetting("upload_concurrent_count",30);
    /*
      # Determines if local files used for temporary storage will be deleted
      # after upload is successful
    */
    public static final PluginConfigSpec<Boolean> DELETE_TEMP_FILES = PluginConfigSpec.booleanSetting("delete_temp_files", true);

    /*
    # Check Proxy URL can be over http or https. Do we need it this way or ignore this & remove this
    */
    public static final PluginConfigSpec<Boolean> DYNAMIC_EVENT_MAPPING = PluginConfigSpec.booleanSetting("dynamic_event_routing",false);

    public static List<PluginConfigSpec<?>> getAllConfigs() {
        PluginConfigSpec<?>[] configs =  new PluginConfigSpec<?>[]{PATH_CONFIG,FLUSH_INTERVAL,FILENAME_FAILURE,RECOVERY,INGEST_URL, APP_ID,APP_KEY,TENANT_ID ,MANAGED_IDENTITY_ID,DATABASE,TABLE,JSON_MAPPING,PROXY_HOST,PROXY_PORT,PROXY_PROTOCOL,UPLOAD_QUEUE_SIZE,UPLOAD_CONCURRENT_COUNT,DELETE_TEMP_FILES, DYNAMIC_EVENT_MAPPING};
        return Arrays.asList(configs);
    }
}
