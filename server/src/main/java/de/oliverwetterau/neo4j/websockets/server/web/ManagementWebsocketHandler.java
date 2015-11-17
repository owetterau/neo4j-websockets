package de.oliverwetterau.neo4j.websockets.server.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by oliver on 13.11.15.
 */
@Service
public class ManagementWebsocketHandler implements WebSocketConnectionCallback {
    private static final Logger logger = LoggerFactory.getLogger(CommandWebsocketHandler.class);

    private final ManagementHandler managementHandler;
    private final JsonObjectMapper jsonObjectMapper;
    private final Set<WebSocketChannel> channels = new HashSet<>();

    @Autowired
    public ManagementWebsocketHandler(ManagementHandler managementHandler, JsonObjectMapper jsonObjectMapper) {
        this.managementHandler = managementHandler;
        this.jsonObjectMapper = jsonObjectMapper;
    }

    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(final WebSocketChannel channel, BufferedTextMessage message) {
                managementHandler.handleMessage(channel, message.getData());

                for (WebSocketChannel session : channel.getPeerConnections()) {
                    WebSockets.sendText(message.getData(), session, null);
                }
            }

            @Override
            protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) throws IOException {
                super.onClose(webSocketChannel, channel);
                channels.remove(webSocketChannel);
            }
        });

        channels.add(channel);
        channel.resumeReceives();
    }

    private Set<WebSocketChannel> getChannels() {
        return Collections.unmodifiableSet(channels);
    }

    public void informAvailableMember(final String memberId, final String role) {
        ObjectNode objectNode = jsonObjectMapper.getObjectMapperBinary().createObjectNode();
        objectNode.put("available", memberId);
        objectNode.put("role", role);

        informAllClients(objectNode);
    }

    public void informUnavailableMember(final String memberId, final String role) {
        ObjectNode objectNode = jsonObjectMapper.getObjectMapperBinary().createObjectNode();
        objectNode.put("unavailable", memberId);
        objectNode.put("role", role);

        informAllClients(objectNode);
    }

    private void informAllClients(final ObjectNode message) {
        for (WebSocketChannel channel : getChannels()) {
            managementHandler.sendMessage(channel, message);
        }
    }
}
