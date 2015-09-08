package de.oliverwetterau.neo4j.websockets.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A single data connection to a Neo4j server.
 *
 * @author Oliver Wetterau
 * @version 2015-02-11
 */
public class DataConnection implements Comparable<DataConnection> {
    private static Logger logger = LoggerFactory.getLogger(DataConnection.class);
    protected static int WEBSOCKET_TIMEOUT = 600; //15;
    protected static long MAXIMUM_AGE_IN_MINUTES = 10;

    protected final WebSocketConnectionManager webSocketConnectionManager;
    protected final WebSocketClient webSocketClient;
    protected final WebSocketHandler webSocketHandler;

    protected final UUID uuid;
    /** uri of Neo4j server to be used */
    protected final String uriTemplate;
    /** date and time of last usage of this connection */
    protected Date lastUsage;

    protected Locale locale;

    /**
     * Constructor
     * @param uriTemplate uri of Neo4j server to connect to
     */
    public DataConnection(final String uriTemplate) {
        this.uuid = UUID.randomUUID();
        this.uriTemplate = uriTemplate;

        this.webSocketHandler = new WebSocketHandler(null, null);
        this.webSocketClient = new StandardWebSocketClient();
        this.webSocketConnectionManager = new WebSocketConnectionManager(webSocketClient, webSocketHandler, uriTemplate);

        lastUsage = new Date();
    }

    /**
     * Initiate connection to Neo4j server.
     * @throws Exception connection could not be established
     */
    public void connect() throws Exception {
        logger.debug("[connect] initializing new connection");

        synchronized (webSocketHandler.getNotifyConnectionObject()) {
            webSocketConnectionManager.start();

            try {
                webSocketHandler.getNotifyConnectionObject().wait(TimeUnit.SECONDS.toMillis(WEBSOCKET_TIMEOUT));
            }
            catch (InterruptedException e) {
                throw new Exception("websocket connection not open");
            }

            if (!isConnected()) {
                throw new Exception("websocket connection timeout (uri = " + uriTemplate + ")");
            }
        }
    }

    /**
     * Close connection to Neo4j server.
     */
    public void close() {
        webSocketConnectionManager.stop();
    }

    /**
     * Get date and time of last usage of this connection.
     * @return date and time of last usage
     */
    public Date getLastUsage() {
        return lastUsage;
    }

    /**
     * Set date and time of last usage of this connection.
     * @param lastUsage date and time of last usage
     */
    public void setLastUsage(final Date lastUsage) {
        this.lastUsage = lastUsage;
    }

    public void setLocale(final Locale locale) {
        this.locale = locale;
        webSocketHandler.setLocale(locale);
    }

    /**
     * Gets whether this connection is still open.
     * @return is this connection still open?
     */
    public boolean isConnected() {
        return webSocketHandler.isConnected();
    }

    /**
     * Gets whether this connection may still be used.
     * @return may this connection still be used?
     */
    public boolean isUsable() {
        return isConnected() && TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - lastUsage.getTime()) <= MAXIMUM_AGE_IN_MINUTES;
    }

    /**
     * Sends a message to the connected Neo4j server without waiting for a reply.
     * @param message binary json message
     */
    public void send(final byte[] message) {
        webSocketHandler.sendMessage(message);
    }

    /**
     * Sends a message to the connected Neo4j server and waits for a reply.
     * @param message binary json message
     * @return result binary json message
     */
    public byte[] sendWithResult(final byte[] message) {
        synchronized (webSocketHandler.getNotifyResultObject()) {
            webSocketHandler.sendMessage(message);

            try {
                webSocketHandler.getNotifyResultObject().wait(TimeUnit.SECONDS.toMillis(WEBSOCKET_TIMEOUT));
            }
            catch (InterruptedException e) {
                return null;
            }

            return webSocketHandler.getResultBytes();
        }
    }

    /**
     * Compares two servers using their Neo4j cluster ids.
     * @param o other server to compare with
     * @return comparison result
     */
    @Override
    public int compareTo(final DataConnection o) {
        return uuid.compareTo(o.uuid);
    }
}

