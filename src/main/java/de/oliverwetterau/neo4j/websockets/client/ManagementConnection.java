package de.oliverwetterau.neo4j.websockets.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * A single management connection to a Neo4j server.
 *
 * @author Oliver Wetterau
 * @version 2015-02-11
 */
public class ManagementConnection implements ConnectionListener {
    private static Logger logger = LoggerFactory.getLogger(ManagementConnection.class);
    protected static int WEBSOCKET_TIMEOUT = 15;
    protected static int WEBSOCKET_RECONNECT_TIMEOUT = 45;

    protected final WebSocketConnectionManager webSocketConnectionManager;
    protected final WebSocketClient webSocketClient;
    protected final WebSocketHandler webSocketHandler;

    protected final ClusterListener clusterListener;
    protected final String uriTemplate;
    protected boolean isAvailable = true;
    protected String serverId = "";

    /** timer used for reconnection attempts */
    protected Timer timer;

    /**
     * Constructor
     * @param uriTemplate uri of Neo4j server to connect to
     */
    public ManagementConnection(final ClusterListener clusterListener, final String uriTemplate) {
        this.clusterListener = clusterListener;
        this.uriTemplate = uriTemplate;

        this.webSocketHandler = new WebSocketHandler(clusterListener, this);
        this.webSocketClient = new StandardWebSocketClient();
        this.webSocketConnectionManager = new WebSocketConnectionManager(webSocketClient, webSocketHandler, uriTemplate);
    }

    public boolean isAvailable() {
        return isAvailable && isConnected();
    }

    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public void setServerId(final String serverId) {
        this.serverId = serverId;
    }

    public String getServerId() {
        return serverId;
    }

    public void connect() throws Exception {
        logger.debug("[connect] initializing new connection");

        if (timer != null) {
            timer.cancel();
        }

        if (isConnected()) {
            return;
        }

        synchronized (webSocketHandler.getNotifyConnectionObject()) {
            webSocketConnectionManager.start();

            try {
                webSocketHandler.getNotifyConnectionObject().wait(TimeUnit.SECONDS.toMillis(WEBSOCKET_TIMEOUT));
            }
            catch (InterruptedException e) {
                onConnectionClosed();
                logger.debug("[connect] not open");
                throw new Exception("websocket connection not open");
            }

            if (!isConnected()) {
                onConnectionClosed();
                logger.debug("[connect] timeout");
                throw new Exception("websocket connection timeout");
            }
        }
    }

    public void onConnectionClosed() {
        logger.debug("[onConnectionClosed] '{}', isConnected = {}", uriTemplate, isConnected());

        webSocketConnectionManager.stop();

        timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.debug("[onConnectionClosed:run]");

                synchronized (webSocketHandler.getNotifyConnectionObject()) {
                    webSocketConnectionManager.start();

                    try {
                        webSocketHandler.getNotifyConnectionObject().wait(TimeUnit.SECONDS.toMillis(WEBSOCKET_RECONNECT_TIMEOUT));
                    } catch (InterruptedException e) {
                        logger.debug("[onConnectionClose]", e);
                    }

                    if (isConnected()) {
                        logger.debug("[onConnectionClosed:run] connected");

                        clusterListener.onServerReconnected(getServerId(), uriTemplate);
                        this.cancel();
                    } else {
                        logger.debug("[onConnectionClosed:run] NOT connected");

                        webSocketConnectionManager.stop();
                    }
                }
            }
        }, TimeUnit.SECONDS.toMillis(WEBSOCKET_TIMEOUT), TimeUnit.SECONDS.toMillis(WEBSOCKET_RECONNECT_TIMEOUT));
    }

    public boolean isConnected() {
        logger.debug("[isConnected] connected = {}", webSocketHandler.isConnected());
        return webSocketHandler.isConnected();
    }

    public void send(final byte[] message) {
        webSocketHandler.sendMessage(message);
    }

    public byte[] sendWithResult(final byte[] message) {
        synchronized (webSocketHandler.getNotifyResultObject()) {
            webSocketHandler.sendMessage(message);

            try {
                webSocketHandler.getNotifyResultObject().wait(5000);
            }
            catch (InterruptedException e) {
                return null;
            }

            return webSocketHandler.getResultBytes();
        }
    }
}

