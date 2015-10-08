package de.oliverwetterau.neo4j.websockets.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.oliverwetterau.neo4j.websockets.core.data.CommandParameters;
import de.oliverwetterau.neo4j.websockets.core.data.Error;
import de.oliverwetterau.neo4j.websockets.core.data.Result;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import de.oliverwetterau.neo4j.websockets.core.i18n.ThreadLocale;
import de.oliverwetterau.neo4j.websockets.server.annotations.MessageController;
import de.oliverwetterau.neo4j.websockets.server.annotations.MessageMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * You should derive a custom command handler from this class and make it a @Service and the constructor @Autowired.
 * MessageControllers should be added to the constructor of the derived class and be added to the list of available
 * controllers by calling @{link setControllerInstance}.
 * To read all annotations correctly the derived class must set 'packageName' to the name of the package where the
 * services with annoations can be found.
 */
public class CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    protected final boolean debugMode;

    protected static final Map<Class,Map<String,Method>> controllerMethods = new HashMap<>();
    protected static final Map<Class,String> controllerServices = new HashMap<>();
    protected static final Map<String,Object> controllers = new HashMap<>();

    protected final JsonObjectMapper jsonObjectMapper;
    protected final ThreadLocale threadLocale;

    protected final String packageName;

    public CommandHandler(final String packageName, final JsonObjectMapper jsonObjectMapper, final ThreadLocale threadLocale, boolean debugMode) throws Exception {
        this.packageName = packageName;
        this.jsonObjectMapper = jsonObjectMapper;
        this.threadLocale = threadLocale;
        this.debugMode = debugMode;

        readMessageControllerAnnotations();

        Result.setJsonObjectMapper(this.jsonObjectMapper);
        Error.setJsonObjectMapper(this.jsonObjectMapper);
    }

    public void handleMessage(final WebSocketSession session, final JsonNode jsonMessage) throws Exception {
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
                    : jsonObjectMapper.getObjectMapper().createObjectNode();
        }
        catch (Exception e) {
            result = new Result<>(e);
            sendMessage(session, result.toJsonBytes());
            return;
        }

        Object controllerInstance = controllers.get(service);

        if (controllerInstance == null) {
            result = new Result<>(new Error(Error.UNKNOWN_SERVICE, service));
            sendMessage(session, result.toJsonBytes());
            return;
        }

        Map<String,Method> methods = controllerMethods.get(controllerInstance.getClass());
        if (debugMode) {
            methods = getMessageMethodAnnotatedMethodsAgain(controllerInstance.getClass());
        }

        if (methods == null) {
            result = new Result<>(new Error(Error.SERVICE_HAS_NO_METHODS, service));
            sendMessage(session, result.toJsonBytes());
            return;
        }

        Method method = methods.get(command);

        if (method == null) {
            ObjectNode detailsNode = jsonObjectMapper.getObjectMapper().createObjectNode();
            detailsNode.put("Service", service);
            detailsNode.put("Command", command);

            result = new Result<>(new Error(
                    Error.UNKNOWN_SERVICE_METHOD,
                    "unknown command '" + command + "' for service '" + service + "'",
                    detailsNode
            ));
            sendMessage(session, result.toJsonBytes());
            return;
        }

        try {
            result = ((Result) method.invoke(controllerInstance, parameters));
            sendMessage(session, result.toJsonBytes());
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            ObjectNode detailsNode = jsonObjectMapper.getObjectMapper().createObjectNode();
            detailsNode.put("Service", service);
            detailsNode.put("Command", command);

            result = new Result<>(new Error(
                    Error.METHOD_EXECUTION_FAILED,
                    "command '" + command + "' for service '" + service + "' could not be executed",
                    detailsNode
            ));
            sendMessage(session, result.toJsonBytes());
        }
    }

    protected class SendMessageRunnable implements Runnable {
        private final WebSocketSession session;
        private final byte[] message;

        public SendMessageRunnable(final WebSocketSession session, final byte[] message) {
            this.session = session;
            this.message = message;
        }

        public void run() {
            try {
                session.sendMessage(new BinaryMessage(message));
            }
            catch (IOException e) {
                JsonNode jsonNode = null;
                try {
                    jsonNode = jsonObjectMapper.getObjectMapper().readTree(message);
                    logger.error("[sendMessage] could not send message: {}", jsonNode.toString(), e);
                }
                catch (IOException ee) {
                    logger.error("[sendMessage] could not convert message to json", ee, e);
                }

            }
        }
    }

    protected void sendMessage(WebSocketSession session, byte[] message) {
        SendMessageRunnable sendMessageRunnable = new SendMessageRunnable(session, message);
        Thread worker = new Thread(sendMessageRunnable);
        worker.start();
    }

    public void setControllerInstance(Object controller) {
        controllers.put(controllerServices.get(controller.getClass()), controller);
    }

    public void readMessageControllerAnnotations() throws Exception {
        Map<String,Method> methods;

        for (final Class clazz : getMessageControllerAnnotatedClasses(packageName)) {
            MessageController messageController = (MessageController) clazz.getAnnotation(MessageController.class);

            controllerServices.put(clazz, messageController.value());

            methods = new HashMap<>();
            controllerMethods.put(clazz, methods);

            for (final Method method : getMessageMethodAnnotatedMethods(clazz)) {
                methods.put(method.getName(), method);
            }
        }
    }

    protected static List<Class<?>> getMessageControllerAnnotatedClasses(String packageName) {
        final List<Class<?>> classes = new ArrayList<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(MessageController.class));
        for (final BeanDefinition candidate : scanner.findCandidateComponents(packageName)) {
            classes.add(ClassUtils.resolveClassName(candidate.getBeanClassName(), ClassUtils.getDefaultClassLoader()));
        }

        return classes;
    }

    protected static List<Method> getMessageMethodAnnotatedMethods(Class clazz) throws Exception {
        Class[] parameters = new Class[] { JsonNode.class };

        final List<Method> methods = new ArrayList<>();

        for (Method method : clazz.getMethods()) {
            MessageMethod messageMethod = method.getAnnotation(MessageMethod.class);
            if (messageMethod != null) {
                if (!Arrays.deepEquals(method.getParameterTypes(), parameters)) {
                    throw new Exception("wrong parameters for @MessageMethod: " + method.toString());
                }
                methods.add(method);
            }
        }

        return methods;
    }

    protected static Map<String,Method> getMessageMethodAnnotatedMethodsAgain(Class clazz) throws Exception {
        List<Method> newMethods = getMessageMethodAnnotatedMethods(clazz);

        if (newMethods == null) {
            return null;
        }

        Map<String,Method> methods = controllerMethods.get(clazz);

        if (methods == null) {
            methods = new HashMap<>();

            controllerMethods.put(clazz, methods);
        }

        for (final Method method : newMethods) {
            if (methods.get(method.getName()) == null) {
                methods.put(method.getName(), method);
            }
        }

        for (final Map.Entry<String,Method> methodEntry : new HashMap<>(methods).entrySet()) {
            if (!newMethods.contains(methodEntry.getValue())) {
                methods.remove(methodEntry.getKey());
            }
        }

        return methods;
    }

    protected static Method getMessageMethodAnnotatedMethod(Class clazz, String name) throws Exception {
        for (Method method : getMessageMethodAnnotatedMethods(clazz)) {
            if (method.getName().equals(name)) {
                return method;
            }
        }

        return null;
    }
}
