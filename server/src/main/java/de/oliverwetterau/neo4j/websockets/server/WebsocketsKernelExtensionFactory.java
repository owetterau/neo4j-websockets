package de.oliverwetterau.neo4j.websockets.server;

import org.neo4j.cluster.member.ClusterMemberEvents;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.ha.HighlyAvailableGraphDatabase;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.neo4j.helpers.Settings.*;
import static org.neo4j.helpers.Settings.STRING;
import static org.neo4j.helpers.Settings.setting;

/**
 * Created by oliver on 13.11.15.
 */
public class WebsocketsKernelExtensionFactory extends KernelExtensionFactory<WebsocketsKernelExtensionFactory.Dependencies> {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketsKernelExtensionFactory.class);

    public interface Dependencies {
        GraphDatabaseService getGraphDatabaseService();
        Config getConfig();
        ClusterMemberEvents getClusterMemberEvents();
    }

    public WebsocketsKernelExtensionFactory() {
        super("WebsocketsExtension");
    }

    @Override
    public Lifecycle newInstance(KernelContext context, Dependencies dependencies) throws Throwable {
        GraphDatabaseService graphDatabaseService = dependencies.getGraphDatabaseService();
        Config config = dependencies.getConfig();

        Setting<HostnamePort> hostnamePort = setting("websocket_host", HOSTNAME_PORT, "0.0.0.0:8765");
        Setting<List<String>> packageNames = setting("websocket_packages", STRING_LIST, "");
        Setting<String> managementPath = setting("websocket_management_path", STRING, "/ws/management");
        Setting<String> commandPath = setting("websocket_data_path", STRING, "/ws/data");

        ClusterMemberEvents clusterMemberEvents = null;

        if (graphDatabaseService instanceof HighlyAvailableGraphDatabase) {
            logger.info("[newInstance] cluster installation");
            clusterMemberEvents = dependencies.getClusterMemberEvents();
        }

        return new WebsocketsKernelExtension(
                graphDatabaseService, clusterMemberEvents, config.get(packageNames), config.get(hostnamePort),
                config.get(managementPath), config.get(commandPath));
    }
}
