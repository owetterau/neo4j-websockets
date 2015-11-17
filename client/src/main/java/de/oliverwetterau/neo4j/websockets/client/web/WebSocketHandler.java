package de.oliverwetterau.neo4j.websockets.client.web;

import com.fasterxml.jackson.databind.JsonNode;
import de.oliverwetterau.neo4j.websockets.client.server.ClusterListener;
import de.oliverwetterau.neo4j.websockets.client.server.ConnectionListener;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Locale;

/**
 * A class to process messages through a websocket connection.
 *
 * @author Oliver Wetterau
 * @version 2015-09-01
 */
public class WebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    protected static JsonObjectMapper jsonObjectMapper;

    protected final ClusterListener clusterListener;
    protected final ConnectionListener connectionListener;

    protected WebSocketSession session;
    protected String resultString;
    protected byte[] resultBytes;
    /** notify object for threads used for waiting for connections */
    protected final Object notifyConnectionObject = new Object();
    /** notify object for threads used for waiting for results */
    protected final Object notifyResultObject = new Object();

    /** language settings */
    protected Locale locale;

    /**
     * Constructor
     * @param clusterListener cluster availablity listener
     * @param connectionListener websocket status listener
     */
    public WebSocketHandler(ClusterListener clusterListener, ConnectionListener connectionListener) {
        this.clusterListener = clusterListener;
        this.connectionListener = connectionListener;
    }

    /**
     * Sets the wrapper for a json object mapper
     * @param jsonObjectMapper wrapper for a json object mapper
     */
    public static void setJsonObjectMapper(JsonObjectMapper jsonObjectMapper) {
        WebSocketHandler.jsonObjectMapper = jsonObjectMapper;
    }

    /**
     * Sets the language settings
     * @param locale language settings
     */
    public void setLocale(final Locale locale) {
        this.locale = locale;
    }

    /**
     * Returns whether this websocket is connected.
     * @return is websocket connected?
     */
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    /**
     * Is called after a websocket session was established.
     * @param webSocketSession websocket session that was established
     */
    @Override
    public void afterConnectionEstablished(final WebSocketSession webSocketSession) {
        logger.debug("[afterConnectionEstablished] id = {}", webSocketSession.getId());
        this.session = webSocketSession;

        synchronized (notifyConnectionObject) {
            notifyConnectionObject.notifyAll();
        }
    }

    /**
     * Is called after a websocket session was closed.
     * @param webSocketSession websocket session that was closed
     * @param status status of the websocket session
     */
    @Override
    public void afterConnectionClosed(final WebSocketSession webSocketSession, final CloseStatus status) {
        logger.debug("[afterConnectionClosed] id = ", webSocketSession.getId());
        this.session = null;

        if (connectionListener != null) {
            connectionListener.onConnectionClosed();
        }
    }

    protected void handleMessage(final Object message, final boolean isBinary) {
        if (isBinary) {
            BinaryMessage binaryMessage = (BinaryMessage) message;
            resultBytes = new byte[binaryMessage.getPayloadLength()];
            binaryMessage.getPayload().get(resultBytes);
        }
        else {
            resultString = ((TextMessage) message).getPayload();
        }

        if (clusterListener != null) {
            JsonNode jsonNode = null;

            try {
                if (isBinary) {
                    jsonNode = jsonObjectMapper.getObjectMapperBinary().readTree(resultBytes);
                }
                else {
                    jsonNode = jsonObjectMapper.getObjectMapperText().readTree(resultString);
                }
            }
            catch (Exception e) {
                logger.error("[handleMessage]", e);
            }

            if (jsonNode != null) {
                if (jsonNode.has("available")) {
                    clusterListener.onServerAvailable(jsonNode.get("available").asText(), jsonNode.get("role").asText());
                }
                else if (jsonNode.has("unavailable")) {
                    clusterListener.onServerUnavailable(jsonNode.get("unavailable").asText());
                }
            }
        }

        synchronized (notifyResultObject) {
            notifyResultObject.notifyAll();
        }
    }

    /**
     * Handles an incoming text messages.
     * @param webSocketSession websocket session the message was received from
     * @param message received message
     */
    @Override
    public void handleTextMessage(final WebSocketSession webSocketSession, final TextMessage message) {
        handleMessage(message, false);
    }

    /**
     * Handles an incoming binary messages.
     * @param webSocketSession websocket session the message was received from
     * @param message received message
     */
    @Override
    public void handleBinaryMessage(final WebSocketSession webSocketSession, final BinaryMessage message) {
        handleMessage(message, true);
    }

    /**
     * Gets the result that was received in {@link #handleTextMessage}
     * @return result that was received in {@link #handleTextMessage}
     */
    public String getResultString() {
        return resultString;
    }

    /**
     * Gets the result that was received in {@link #handleBinaryMessage}
     * @return result that was received in {@link #handleBinaryMessage}
     */
    public byte[] getResultBytes() {
        return resultBytes;
    }

    /**
     * Handles websocket transport errors
     * @param webSocketSession websocket session where the error appeared
     * @param exception exception that occured
     * @throws Exception transport error exception
     */
    @Override
    public void handleTransportError(final WebSocketSession webSocketSession, final Throwable exception) throws Exception {
        if (exception != null) {
            logger.error("[handleTransportError]", exception);
        }
    }

    /**
     * Sends a text message using this object's websocket session.
     * @param message json binary message
     */
    public void sendMessage(final String message) {
        try {
            session.sendMessage(new TextMessage(message));
        }
        catch (IOException e) {
            logger.error("[sendTextMessage]", e);
        }
    }

    /**
     * Sends a binary message using this object's websocket session.
     * @param message json binary message
     */
    public void sendMessage(final byte[] message) {
        try {
            session.sendMessage(new BinaryMessage(message));
        }
        catch (IOException e) {
            logger.error("[sendBinaryMessage]", e);
        }
    }

    /**
     * Returns the synchronisation object used for connections.
     * @return synchronisation object used for connections
     */
    public Object getNotifyConnectionObject() {
        return notifyConnectionObject;
    }

    /**
     * Returns the synchronisation object used for results of a websocket message.
     * @return synchronisation object used for results of a websocket message
     */
    public Object getNotifyResultObject() {
        return notifyResultObject;
    }
}
