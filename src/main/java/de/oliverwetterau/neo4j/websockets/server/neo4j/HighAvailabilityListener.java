package de.oliverwetterau.neo4j.websockets.server.neo4j;

import de.oliverwetterau.neo4j.websockets.server.ManagementWebSocketHandler;
import org.neo4j.cluster.InstanceId;
import org.neo4j.cluster.member.ClusterMemberListener;
import org.neo4j.kernel.ha.cluster.HighAvailabilityMemberChangeEvent;
import org.neo4j.kernel.ha.cluster.HighAvailabilityMemberListener;
import org.neo4j.kernel.impl.store.StoreId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Created by oliver on 25.08.15.
 */
public class HighAvailabilityListener implements HighAvailabilityMemberListener, ClusterMemberListener {
    private static final Logger logger = LoggerFactory.getLogger(HighAvailabilityListener.class);

    public class AvailableMember implements Runnable {
        protected final String id;
        protected final String role;

        public AvailableMember(String id, String role) {
            this.id = id;
            this.role = role;
        }

        public void run() {
            for (;managementWebSocketHandler == null;){
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                }
            }

            managementWebSocketHandler.informAvailableMember(id, role);
        }
    }

    public class UnavailableMember implements Runnable {
        protected final String id;
        protected final String role;

        public UnavailableMember(String id, String role) {
            this.id = id;
            this.role = role;
        }

        public void run() {
            for (;managementWebSocketHandler == null;){
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                }
            }

            managementWebSocketHandler.informUnavailableMember(id, role);
        }
    }

    protected ManagementWebSocketHandler managementWebSocketHandler;

    public void setManagementWebSocketHandler(ManagementWebSocketHandler managementWebSocketHandler) {
        this.managementWebSocketHandler = managementWebSocketHandler;
    }

    public void coordinatorIsElected(InstanceId instanceId) {
    }

    public void memberIsAvailable(String role, InstanceId instanceId, URI uri, StoreId storeId) {
        logger.debug("[memberIsAvailable] id = {}, uri = {}, role = {}", instanceId, uri, role);
        new Thread(new AvailableMember(instanceId.toString(), role)).start();
        //managementWebSocketHandler.informAvailableMember(instanceId.toString(), role);
    }

    public void memberIsUnavailable(String role, InstanceId instanceId) {
        logger.debug("[memberIsUnavailable] id = {}, uri = {}, role = {}", instanceId, role);
        new Thread(new UnavailableMember(instanceId.toString(), role)).start();
        //managementWebSocketHandler.informUnavailableMember(instanceId.toString(), s);
    }

    public void memberIsFailed(InstanceId instanceId) {
        logger.debug("[memberIsFailed] id = {}", instanceId);
    }

    public void memberIsAlive(InstanceId instanceId) {
        logger.debug("[memberIsAlive] id = {}", instanceId);
    }

    public void masterIsElected(HighAvailabilityMemberChangeEvent highAvailabilityMemberChangeEvent) {
        logger.debug("[masterIsElected] id = {}, uri = {}", highAvailabilityMemberChangeEvent.getInstanceId(), highAvailabilityMemberChangeEvent.getServerHaUri());
    }

    public void masterIsAvailable(HighAvailabilityMemberChangeEvent highAvailabilityMemberChangeEvent) {
        logger.debug("[masterIsAvailable] id = {}, uri = {}", highAvailabilityMemberChangeEvent.getInstanceId(), highAvailabilityMemberChangeEvent.getServerHaUri());
    }

    public void slaveIsAvailable(HighAvailabilityMemberChangeEvent highAvailabilityMemberChangeEvent) {
        logger.debug("[slaveIsAvailable] id = {}, uri = {}", highAvailabilityMemberChangeEvent.getInstanceId(), highAvailabilityMemberChangeEvent.getServerHaUri());
    }

    public void instanceStops(HighAvailabilityMemberChangeEvent highAvailabilityMemberChangeEvent) {
        logger.debug("[instanceStops] id = {}, uri = {}", highAvailabilityMemberChangeEvent.getInstanceId(), highAvailabilityMemberChangeEvent.getServerHaUri());
    }
}
