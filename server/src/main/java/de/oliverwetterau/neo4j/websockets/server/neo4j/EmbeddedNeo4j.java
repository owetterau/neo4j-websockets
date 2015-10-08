package de.oliverwetterau.neo4j.websockets.server.neo4j;

import de.oliverwetterau.neo4j.websockets.server.ManagementWebSocketHandler;
import org.neo4j.cluster.member.ClusterMemberEvents;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.ha.HighlyAvailableGraphDatabase;
import org.neo4j.kernel.ha.cluster.HighAvailability;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Created by oliver on 25.08.15.
 */
public class EmbeddedNeo4j {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedNeo4j.class);

    @SuppressWarnings("deprecation")
    private static WrappingNeoServerBootstrapper serverBootstrapper;

    protected static EmbeddedNeo4j instance;
    protected HighAvailability highAvailability;
    protected ClusterMemberEvents clusterMemberEvents;
    protected HighAvailabilityListener highAvailabilityListener = null;

    protected Configurer configurer;

    public void initialize(final Configurer configurer) {
        this.configurer = configurer;
    }

    public void start() {
        boolean isHighlyAvailable;
        boolean isOkToWrite;

        GraphDatabaseService databaseService = getDatabase();

        isHighlyAvailable = databaseService instanceof HighlyAvailableGraphDatabase;

        isOkToWrite = !isHighlyAvailable || ((HighlyAvailableGraphDatabase) databaseService).isMaster();

        if (isOkToWrite) {
            startSchemaCreation(databaseService);
        }

        if (isHighlyAvailable) {
            //noinspection deprecation
            serverBootstrapper = new WrappingNeoServerBootstrapper(
                    (GraphDatabaseAPI) databaseService,
                    configurer.getServerConfigurator((HighlyAvailableGraphDatabase) databaseService));
            serverBootstrapper.start();
        }
    }

    public void startSchemaCreation(final GraphDatabaseService databaseService) {
    }

    public void stop() {
        if (serverBootstrapper != null) {
            serverBootstrapper.stop();
            serverBootstrapper = null;
        }

        if (highAvailability != null) {
            highAvailability.removeHighAvailabilityMemberListener(highAvailabilityListener);
        }
        if (clusterMemberEvents != null) {
            clusterMemberEvents.removeClusterMemberListener(highAvailabilityListener);
        }

        configurer.dropGraphDatabase();
    }

    public GraphDatabaseService getDatabase() {
        return configurer.getGraphDatabase();
    }

    public void setHighAvailabilityListener(final HighAvailabilityListener highAvailabilityListener) {
        GraphDatabaseService databaseService = getDatabase();

        this.highAvailabilityListener = highAvailabilityListener;

        try {
            Field memberStateMachineField = databaseService.getClass().getDeclaredField("memberStateMachine");
            memberStateMachineField.setAccessible(true);
            highAvailability = (HighAvailability) memberStateMachineField.get(databaseService);
            clusterMemberEvents = ((HighlyAvailableGraphDatabase) databaseService).getDependencyResolver().resolveDependency(ClusterMemberEvents.class);

            highAvailability.addHighAvailabilityMemberListener(highAvailabilityListener);
            clusterMemberEvents.addClusterMemberListener(highAvailabilityListener);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("[setHighAvailabilityListener]", e);
        }
    }

    public void addManagementWebSocketHandlerToHighAvailabilityListener(final ManagementWebSocketHandler managementWebSocketHandler) {
        if (highAvailabilityListener == null) {
            return;
        }

        highAvailabilityListener.setManagementWebSocketHandler(managementWebSocketHandler);
    }

    public boolean isMaster() {
        GraphDatabaseService databaseService = getDatabase();

        return !(databaseService instanceof HighlyAvailableGraphDatabase)
                || ((HighlyAvailableGraphDatabase) databaseService).isMaster();
    }

    public Integer getHighAvailabilityId() {
        return configurer.getHighAvailabilityId();
    }
}
