package de.oliverwetterau.neo4j.websockets.server;

import de.oliverwetterau.neo4j.websockets.server.aspects.DatabaseCallAspect;
import de.oliverwetterau.neo4j.websockets.server.ha.HighAvailabilityConfiguration;
import org.neo4j.cluster.member.ClusterMemberEvents;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by oliver on 13.11.15.
 */
public class WebsocketsKernelExtension implements Lifecycle {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketsKernelExtension.class);

    private final WebsocketServer websocketServer;

    public WebsocketsKernelExtension(
            GraphDatabaseService graphDatabaseService, ClusterMemberEvents clusterMemberEvents,
            List<String> packageNames, HostnamePort hostnamePort, String managementPath, String dataPath)
            throws Exception
    {
        logger.info("[Constructor] package names = '{}', port = '{}', management path = '{}', data path = '{}'",
                packageNames, hostnamePort.getPort(), managementPath, dataPath);

        DatabaseConfiguration.setGraphDatabaseService(graphDatabaseService);

        DatabaseCallAspect.setGraphDatabaseService(graphDatabaseService);

        ApplicationSettings.configure(packageNames, hostnamePort.getHost(), hostnamePort.getPort(), managementPath, dataPath);

        HighAvailabilityConfiguration.instance().configure(graphDatabaseService, clusterMemberEvents);

        websocketServer = new WebsocketServer();
    }

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void start() throws Throwable {
        websocketServer.run();
    }

    @Override
    public void stop() throws Throwable {
        websocketServer.stop();
    }

    @Override
    public void shutdown() throws Throwable {
        //websocketServer.stop();
    }
}
