package de.oliverwetterau.neo4j.websockets.client;

import com.fasterxml.jackson.databind.JsonNode;
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
 * Created by oliver on 26.08.15.
 */
public class WebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    protected static JsonObjectMapper jsonObjectMapper;

    protected final ClusterListener clusterListener;
    protected final ConnectionListener connectionListener;

    protected WebSocketSession session;
    protected String resultString;
    protected byte[] resultBytes;
    protected final Object notifyConnectionObject = new Object();
    protected final Object notifyResultObject = new Object();

    protected Locale locale;

    public WebSocketHandler(ClusterListener clusterListener, ConnectionListener connectionListener) {
        this.clusterListener = clusterListener;
        this.connectionListener = connectionListener;
    }

    public static void setJsonObjectMapper(JsonObjectMapper jsonObjectMapper) {
        WebSocketHandler.jsonObjectMapper = jsonObjectMapper;
    }

    public void setLocale(final Locale locale) {
        this.locale = locale;
    }

    public byte[] getResultBytes() {
        return resultBytes;
    }

    public Object getNotifyConnectionObject() {
        return notifyConnectionObject;
    }

    public Object getNotifyResultObject() {
        return notifyResultObject;
    }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    @Override
    public void afterConnectionEstablished(final WebSocketSession webSocketSession) {
        logger.debug("[afterConnectionEstablished] id = {}", webSocketSession.getId());
        this.session = webSocketSession;

        synchronized (notifyConnectionObject) {
            notifyConnectionObject.notifyAll();
        }
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession webSocketSession, final CloseStatus status) {
        logger.debug("[afterConnectionClosed] id = ", webSocketSession.getId());
        this.session = null;

        if (connectionListener != null) {
            connectionListener.onConnectionClosed();
        }
    }

    @Override
    public void handleTextMessage(final WebSocketSession webSocketSession, final TextMessage message) {
        resultString = message.getPayload();

        if (clusterListener != null) {
            JsonNode jsonNode = null;

            try {
                jsonNode = jsonObjectMapper.getObjectMapper().readTree(resultString);
            }
            catch (Exception e) {
                logger.error("[handleTextMessage]", e);
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

    @Override
    public void handleBinaryMessage(final WebSocketSession webSocketSession, final BinaryMessage message) {
        resultBytes = new byte[message.getPayloadLength()];
        message.getPayload().get(resultBytes);

        if (clusterListener != null) {
            JsonNode jsonNode = null;

            try {
                jsonNode = jsonObjectMapper.getObjectMapper().readTree(resultBytes);
            }
            catch (Exception e) {
                logger.error("[handleBinaryMessage]", e);
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

    @Override
    public void handleTransportError(final WebSocketSession session, final Throwable exception) throws Exception {
        if (exception != null) {
            logger.error("[handleTransportError]", exception);
        }
    }

    public void sendMessage(final byte[] message) {
        try {
            session.sendMessage(new BinaryMessage(message));
        }
        catch (IOException e) {
            logger.error("[sendMessage(byte[])]", e);
        }
    }
}
