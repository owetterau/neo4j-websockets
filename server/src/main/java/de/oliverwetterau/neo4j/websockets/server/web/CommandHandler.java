package de.oliverwetterau.neo4j.websockets.server.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.oliverwetterau.neo4j.websockets.server.annotations.AnnotationReader;
import de.oliverwetterau.neo4j.websockets.server.neo4j.ExceptionToErrorConverter;
import de.oliverwetterau.neo4j.websockets.core.data.CommandParameters;
import de.oliverwetterau.neo4j.websockets.core.data.Error;
import de.oliverwetterau.neo4j.websockets.core.data.Result;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import de.oliverwetterau.neo4j.websockets.core.helpers.ThreadBinary;
import de.oliverwetterau.neo4j.websockets.core.i18n.ThreadLocale;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;

/**
 * Created by oliver on 13.11.15.
 */
@Service
public class CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);

    private final JsonObjectMapper jsonObjectMapper;
    private final ThreadLocale threadLocale;
    private final AnnotationReader annotationReader;
    private final ExceptionToErrorConverter exceptionToErrorConverter;

    @Autowired
    public CommandHandler(JsonObjectMapper jsonObjectMapper, ThreadLocale threadLocale,
                          AnnotationReader annotationReader, ExceptionToErrorConverter exceptionToErrorConverter)
    {
        this.jsonObjectMapper = jsonObjectMapper;
        this.threadLocale = threadLocale;
        this.annotationReader = annotationReader;
        this.exceptionToErrorConverter = exceptionToErrorConverter;

        Result.setJsonObjectMapper(this.jsonObjectMapper);
        Error.setJsonObjectMapper(this.jsonObjectMapper);
    }

    public void handleTextMessage(final WebSocketChannel channel, final JsonNode jsonMessage) throws Exception {
        ThreadBinary.setBinary(false);
        sendTextMessage(channel, handleMessage(jsonMessage).toJsonString());
    }

    public void handleBinaryMessage(final WebSocketChannel channel, final JsonNode jsonMessage) throws Exception {
        ThreadBinary.setBinary(true);
        sendBinaryMessage(channel, handleMessage(jsonMessage).toJsonBytes());
    }

    protected Result handleMessage(final JsonNode jsonMessage) throws Exception {
        ObjectMapper objectMapper = jsonObjectMapper.getObjectMapper();
        String service;
        String command;
        JsonNode parameters;
        Locale locale;

        Result result;

        try {
            if (jsonMessage.has(CommandParameters.LANGUAGE)) {
                locale = new Locale(jsonMessage.get(CommandParameters.LANGUAGE).asText());
            } else {
                locale = Locale.US;
            }
            threadLocale.setLocale(locale);

            service = jsonMessage.has(CommandParameters.SERVICE) ? jsonMessage.get(CommandParameters.SERVICE).asText() : "";
            command = jsonMessage.has(CommandParameters.METHOD) ? jsonMessage.get(CommandParameters.METHOD).asText() : "";
            parameters = jsonMessage.has(CommandParameters.PARAMETERS)
                    ? jsonMessage.get(CommandParameters.PARAMETERS)
                    : objectMapper.createObjectNode();
        }
        catch (Exception e) {
            return new Result<>(exceptionToErrorConverter.convert(e));
        }

        Object controllerInstance = annotationReader.getServiceController(service);

        if (controllerInstance == null) {
            return new Result<>(new de.oliverwetterau.neo4j.websockets.core.data.Error(Error.UNKNOWN_SERVICE, service));
        }

        Map<String,Method> methods = annotationReader.getControllerMethods(controllerInstance.getClass());

        if (methods == null) {
            return new Result<>(new Error(Error.SERVICE_HAS_NO_METHODS, service));
        }

        Method method = methods.get(command);

        if (method == null) {
            ObjectNode detailsNode = objectMapper.createObjectNode();
            detailsNode.put("Service", service);
            detailsNode.put("Command", command);

            result = new Result<>(new Error(
                    Error.UNKNOWN_SERVICE_METHOD,
                    "unknown command '" + command + "' for service '" + service + "'",
                    detailsNode
            ));

            return result;
        }

        try {
            return ((Result) method.invoke(controllerInstance, parameters));
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            ObjectNode detailsNode = objectMapper.createObjectNode();
            detailsNode.put("Service", service);
            detailsNode.put("Command", command);

            result = new Result<>(new Error(
                    Error.METHOD_EXECUTION_FAILED,
                    "command '" + command + "' for service '" + service + "' could not be executed",
                    detailsNode
            ));

            return result;
        }
    }

    private void sendTextMessage(final WebSocketChannel channel, final String message) {
        SendTextMessageRunnable sendMessageRunnable = new SendTextMessageRunnable(channel, message);
        Thread worker = new Thread(sendMessageRunnable);
        worker.start();
    }

    private class SendTextMessageRunnable implements Runnable {
        private final WebSocketChannel channel;
        private final String message;

        public SendTextMessageRunnable(final WebSocketChannel channel, final String message) {
            this.channel = channel;
            this.message = message;
        }

        public void run() {
            WebSockets.sendText(message, channel, null);
        }
    }

    private void sendBinaryMessage(WebSocketChannel channel, byte[] message) {
        SendBinaryMessageRunnable sendMessageRunnable = new SendBinaryMessageRunnable(channel, message);
        Thread worker = new Thread(sendMessageRunnable);
        worker.start();
    }

    private class SendBinaryMessageRunnable implements Runnable {
        private final WebSocketChannel channel;
        private final byte[] message;

        public SendBinaryMessageRunnable(final WebSocketChannel channel, final byte[] message) {
            this.channel = channel;
            this.message = message;
        }

        public void run() {
            WebSockets.sendBinary(ByteBuffer.wrap(message), channel, null);
        }
    }
}
