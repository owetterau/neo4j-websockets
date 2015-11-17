package de.oliverwetterau.neo4j.websockets.server.ha;

import de.oliverwetterau.neo4j.websockets.server.web.ManagementWebsocketHandler;
import org.neo4j.cluster.InstanceId;
import org.neo4j.cluster.member.ClusterMemberListener;
import org.neo4j.kernel.impl.store.StoreId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Created by oliver on 10.11.15.
 */
public class HighAvailabilityListener implements ClusterMemberListener {
    private static final Logger logger = LoggerFactory.getLogger(HighAvailabilityListener.class);

    public class AvailableMember implements Runnable {
        private final String id;
        private final String role;

        public AvailableMember(String id, String role) {
            this.id = id;
            this.role = role;
        }

        public void run() {
            while (managementWebsocketHandler != null){
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException ignored) {}
            }

            managementWebsocketHandler.informAvailableMember(id, role);
        }
    }

    public class UnavailableMember implements Runnable {
        private final String id;
        private final String role;

        public UnavailableMember(String id, String role) {
            this.id = id;
            this.role = role;
        }

        public void run() {
            while (managementWebsocketHandler != null){
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException ignored) {}
            }

            managementWebsocketHandler.informUnavailableMember(id, role);
        }
    }

    private ManagementWebsocketHandler managementWebsocketHandler;

    public HighAvailabilityListener(final ManagementWebsocketHandler managementWebSocketHandler) {
        this.managementWebsocketHandler = managementWebSocketHandler;
    }

    @Override
    public void coordinatorIsElected(InstanceId instanceId) {
        logger.debug("[coordinatorIsElected] id = {}", instanceId);
    }

    @Override
    public void memberIsAvailable(String role, InstanceId instanceId, URI uri, StoreId storeId) {
        logger.debug("[memberIsAvailable] id = {}, uri = {}, role = {}", instanceId, uri, role);

        new Thread(new AvailableMember(instanceId.toString(), role)).start();
    }

    @Override
    public void memberIsUnavailable(String role, InstanceId instanceId) {
        logger.debug("[memberIsUnavailable] id = {}, uri = {}, role = {}", instanceId, role);

        new Thread(new UnavailableMember(instanceId.toString(), role)).start();
    }

    @Override
    public void memberIsFailed(InstanceId instanceId) {
        logger.debug("[memberIsFailed] id = {}", instanceId);
    }

    @Override
    public void memberIsAlive(InstanceId instanceId) {
        logger.debug("[memberIsAlive] id = {}", instanceId);
    }
}
