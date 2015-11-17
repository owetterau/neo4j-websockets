package de.oliverwetterau.neo4j.websockets.server.web;

import com.fasterxml.jackson.databind.JsonNode;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import de.oliverwetterau.neo4j.websockets.core.helpers.ExceptionConverter;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xnio.Pooled;

import java.nio.ByteBuffer;

/**
 * Created by oliver on 13.11.15.
 */
@Service
public class CommandWebsocketHandler implements WebSocketConnectionCallback {
    private static final Logger logger = LoggerFactory.getLogger(CommandWebsocketHandler.class);

    private final CommandHandler commandHandler;
    private final JsonObjectMapper jsonObjectMapper;

    @Autowired
    public CommandWebsocketHandler(CommandHandler commandHandler, JsonObjectMapper jsonObjectMapper) {
        this.commandHandler = commandHandler;
        this.jsonObjectMapper = jsonObjectMapper;
    }

    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        logger.info("[onConnect]");

        channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullBinaryMessage(final WebSocketChannel channel, BufferedBinaryMessage message) {
                logger.info("[onFullBinaryMessage]");

                Pooled<ByteBuffer[]> messageData = message.getData();
                try {
                    ByteBuffer[] resource = messageData.getResource();
                    ByteBuffer byteBuffer = WebSockets.mergeBuffers(resource);

                    JsonNode jsonNode = jsonObjectMapper.getObjectMapperBinary().readTree(byteBuffer.array());

                    commandHandler.handleBinaryMessage(channel, jsonNode);
                }
                catch (Exception e) {
                    logger.error("[onFullBinaryMessage] {} \n => {}", e, ExceptionConverter.stackTrace(e));
                }
                finally {
                    messageData.discard();
                }
            }

            @Override
            protected void onFullTextMessage(final WebSocketChannel channel, BufferedTextMessage message) {
                logger.info("[onFullTextMessage]");

                try {
                    JsonNode jsonNode = jsonObjectMapper.getObjectMapperText().readTree(message.getData());

                    commandHandler.handleTextMessage(channel, jsonNode);
                }
                catch (Exception e) {
                    logger.error("[onFullTextMessage] {} \n => {}", e, ExceptionConverter.stackTrace(e));
                }
            }
        });

        channel.resumeReceives();
    }
}
