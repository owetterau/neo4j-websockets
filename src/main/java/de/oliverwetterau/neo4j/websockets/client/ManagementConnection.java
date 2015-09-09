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
 * @version 2015-09-01
 */
public class ManagementConnection implements ConnectionListener {
    private static Logger logger = LoggerFactory.getLogger(ManagementConnection.class);
    protected static int WEBSOCKET_TIMEOUT = 15;
    protected static int WEBSOCKET_RECONNECT_TIMEOUT = 45;
    protected static int ANSWER_TIMEOUT = 5;

    protected final WebSocketConnectionManager webSocketConnectionManager;
    protected final WebSocketClient webSocketClient;
    protected final WebSocketHandler webSocketHandler;

    protected final ClusterListener clusterListener;
    /** uri of Neo4j server to connect to */
    protected final String uri;
    /** is this server connection available? */
    protected boolean isAvailable = true;
    /** the id of the Neo4j server */
    protected String serverId = "";

    /** timer used for reconnection attempts */
    protected Timer timer;

    /**
     * Constructor
     * @param clusterListener listener for cluster events
     * @param uri uri of Neo4j server to connect to
     */
    public ManagementConnection(final ClusterListener clusterListener, final String uri) {
        this.clusterListener = clusterListener;
        this.uri = uri;

        this.webSocketHandler = new WebSocketHandler(clusterListener, this);
        this.webSocketClient = new StandardWebSocketClient();
        this.webSocketConnectionManager = new WebSocketConnectionManager(webSocketClient, webSocketHandler, uri);
    }

    /**
     * Returns whether this server connection is available
     * @return availability of this server connection
     */
    public boolean isAvailable() {
        return isAvailable && isConnected();
    }

    /**
     * Sets whether this server connection is available
     * @param isAvailable availability of this server connection
     */
    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    /**
     * Sets the id of the Neo4j server this connection is linked to
     * @param serverId id of the Neo4j server
     */
    public void setServerId(final String serverId) {
        this.serverId = serverId;
    }

    /**
     * Gets the id of the Neo4j server this connection is linked to
     * @return id of the Neo4j server
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * Connects to the Neo4j server identified by URI and ID of this connection.
     * @throws Exception websocket timeout exception
     */
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

    /**
     * Is being called when the websocket connection was closed and tries to reconnect.
     */
    public void onConnectionClosed() {
        logger.debug("[onConnectionClosed] '{}', isConnected = {}", uri, isConnected());

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

                        clusterListener.onServerReconnected(getServerId(), uri);
                        this.cancel();
                    } else {
                        logger.debug("[onConnectionClosed:run] NOT connected");

                        webSocketConnectionManager.stop();
                    }
                }
            }
        }, TimeUnit.SECONDS.toMillis(WEBSOCKET_TIMEOUT), TimeUnit.SECONDS.toMillis(WEBSOCKET_RECONNECT_TIMEOUT));
    }

    /**
     * Returns whether this connection has an active websocket connection to its server.
     * @return does this connection have an active websocket connection?
     */
    public boolean isConnected() {
        logger.debug("[isConnected] connected = {}", webSocketHandler.isConnected());
        return webSocketHandler.isConnected();
    }

    /**
     * Sends a message through it's websocket connection to the server.
     * @param message json message in binary format
     */
    public void send(final byte[] message) {
        webSocketHandler.sendMessage(message);
    }

    /**
     * Sends a message through it's websocket connection to the server and waits for the reply.
     * @param message json message in binary format
     * @return answer from the server in json binary format
     */
    public byte[] sendWithResult(final byte[] message) {
        synchronized (webSocketHandler.getNotifyResultObject()) {
            webSocketHandler.sendMessage(message);

            try {
                webSocketHandler.getNotifyResultObject().wait(TimeUnit.SECONDS.toMillis(ANSWER_TIMEOUT));
            }
            catch (InterruptedException e) {
                return null;
            }

            return webSocketHandler.getResultBytes();
        }
    }
}

