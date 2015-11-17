package de.oliverwetterau.neo4j.websockets.server;

import de.oliverwetterau.neo4j.websockets.server.annotations.StartListener;
import de.oliverwetterau.neo4j.websockets.server.json.DefaultJsonObjectSerializers;
import de.oliverwetterau.neo4j.websockets.server.web.CommandWebsocketHandler;
import de.oliverwetterau.neo4j.websockets.server.web.DefaultThreadLocale;
import de.oliverwetterau.neo4j.websockets.server.web.ManagementWebsocketHandler;
import io.undertow.Undertow;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.websocket;

/**
 * Created by oliver on 13.11.15.
 */
public class WebsocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketServer.class);

    private Undertow webServer;
    private AnnotationConfigApplicationContext applicationContext;

    public void run() throws Exception {
        logger.info("[run]");

        applicationContext = new AnnotationConfigApplicationContext();
        contextScan(applicationContext);
        registgerStartListener(applicationContext);

        ManagementWebsocketHandler managementWebsocketHandler = applicationContext.getBean(ManagementWebsocketHandler.class);
        CommandWebsocketHandler commandWebsocketHandler = applicationContext.getBean(CommandWebsocketHandler.class);

        webServer = Undertow.builder()
                .addHttpListener(ApplicationSettings.port(), ApplicationSettings.host())
                .setHandler(path()
                        .addPrefixPath(ApplicationSettings.managementPath(), websocket(managementWebsocketHandler))
                        .addPrefixPath(ApplicationSettings.dataPath(), websocket(commandWebsocketHandler)))
                .build();

        runStartListener(applicationContext);

        webServer.start();
    }

    private void contextScan(AnnotationConfigApplicationContext applicationContext)
            throws ClassNotFoundException
    {
        applicationContext.scan("de.oliverwetterau.neo4j.websockets");

        for (String packageName : ApplicationSettings.packageNames()) {
            applicationContext.scan(packageName);
        }

        registerDefaultClasses(applicationContext);

        applicationContext.refresh();
    }

    private void registerDefaultClasses(AnnotationConfigApplicationContext applicationContext) {
        Class jsonObjectSerializersClass = ApplicationSettings.jsonObjectSerializersClass();

        if (jsonObjectSerializersClass == null) {
            applicationContext.register(DefaultJsonObjectSerializers.class);
        }
        else {
            applicationContext.register(jsonObjectSerializersClass);
        }

        Class threadLocaleClass = ApplicationSettings.threadLocaleClass();

        if (threadLocaleClass == null) {
            applicationContext.register(DefaultThreadLocale.class);
        }
        else {
            applicationContext.register(threadLocaleClass);
        }
    }

    private void registgerStartListener(AnnotationConfigApplicationContext applicationContext) {
        if (ApplicationSettings.startListenerClass() != null) {
            applicationContext.register(ApplicationSettings.startListenerClass());
        }
    }

    private void runStartListener(AnnotationConfigApplicationContext applicationContext) {
        if (ApplicationSettings.startListenerClass() != null) {
            StartListener instance = (StartListener) applicationContext.getBean(ApplicationSettings.startListenerClass());

            GraphDatabaseService graphDatabaseService = applicationContext.getBean(GraphDatabaseService.class);
            instance.onStart(graphDatabaseService, webServer);
        }
    }

    public void stop() {
        webServer.stop();
        applicationContext.stop();
    }
}
