package de.oliverwetterau.neo4j.websockets.server.ha;

import org.neo4j.cluster.member.ClusterMemberEvents;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.jmx.JmxUtils;
import org.neo4j.kernel.ha.HighlyAvailableGraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;

/**
 * Created by oliver on 10.11.15.
 */
public class HighAvailabilityConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(HighAvailabilityConfiguration.class);

    private static HighAvailabilityConfiguration instance;

    private GraphDatabaseService databaseService;
    private boolean highlyAvailable;
    private Integer serverId = null;

    private ClusterMemberEvents clusterMemberEvents;
    private HighAvailabilityListener highAvailabilityListener = null;

    private HighAvailabilityConfiguration() {
    }

    public static HighAvailabilityConfiguration instance() {
        if (instance == null) {
            instance = new HighAvailabilityConfiguration();
        }

        return instance;
    }

    public void configure(GraphDatabaseService databaseService, ClusterMemberEvents clusterMemberEvents) {
        instance().databaseService = databaseService;
        instance().highlyAvailable = databaseService instanceof HighlyAvailableGraphDatabase;
        instance().clusterMemberEvents = clusterMemberEvents;
    }

    public boolean isHighlyAvailable() {
        return highlyAvailable;
    }

    public void setHighAvailabilityListener(final HighAvailabilityListener highAvailabilityListener) {
        this.highAvailabilityListener = highAvailabilityListener;
        clusterMemberEvents.addClusterMemberListener(highAvailabilityListener);
    }

    public void stop() {
        if (clusterMemberEvents != null) {
            clusterMemberEvents.removeClusterMemberListener(highAvailabilityListener);
        }
    }

    public Integer getId() {
        if (serverId == null) {
            try {
                ObjectName objectName = JmxUtils.getObjectName(databaseService, "High Availability");
                serverId = Integer.parseInt((String) JmxUtils.getAttribute(objectName, "InstanceId"));
            }
            catch (Exception e) {
                serverId = -1;
            }
        }

        return serverId;
    }

    public boolean isMaster() {
        return highlyAvailable && ((HighlyAvailableGraphDatabase) databaseService).isMaster();
    }
}
