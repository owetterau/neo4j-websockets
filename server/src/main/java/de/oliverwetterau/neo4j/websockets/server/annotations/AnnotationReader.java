package de.oliverwetterau.neo4j.websockets.server.annotations;

import com.fasterxml.jackson.databind.JsonNode;
import de.oliverwetterau.neo4j.websockets.core.data.Result;
import de.oliverwetterau.neo4j.websockets.server.ApplicationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by oliver on 10.11.15.
 */
@Service
public class AnnotationReader {
    private static final Logger logger = LoggerFactory.getLogger(AnnotationReader.class);

    private final ApplicationContext applicationContext;
    private final Map<String,Object> controllers = new HashMap<>();
    private final Map<Class,String> controllerServices = new HashMap<>();
    private final Map<Class,Map<String,Method>> controllerMethods = new HashMap<>();

    private boolean initialized = false;

    @Autowired
    public AnnotationReader(ApplicationContext applicationContext) throws Exception {
        this.applicationContext = applicationContext;

        for (String packageName : ApplicationSettings.packageNames()) {
            readMessageControllerAnnotations(packageName);
        }
    }

    public Object getServiceController(String name) {
        init();
        return controllers.get(name);
    }

    public Map<String,Method> getControllerMethods(Class controller) {
        init();
        return controllerMethods.get(controller);
    }

    private void init() {
        if (initialized) return;

        for (Map.Entry<Class,String> controllerAssignment : controllerServices.entrySet()) {
            Object controllerInstance = applicationContext.getBean(controllerAssignment.getKey());
            controllers.put(controllerAssignment.getValue(), controllerInstance);
        }

        initialized = true;
    }

    private void readMessageControllerAnnotations(String packageName) throws Exception {
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

    private List<Class<?>> getMessageControllerAnnotatedClasses(String packageName) {
        final List<Class<?>> classes = new ArrayList<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(MessageController.class));
        for (final BeanDefinition candidate : scanner.findCandidateComponents(packageName)) {
            classes.add(ClassUtils.resolveClassName(candidate.getBeanClassName(), ClassUtils.getDefaultClassLoader()));
        }

        return classes;
    }

    private List<Method> getMessageMethodAnnotatedMethods(Class clazz) throws Exception {
        Class[] parameters = new Class[] { JsonNode.class };

        final List<Method> methods = new ArrayList<>();

        for (Method method : clazz.getMethods()) {
            MessageMethod messageMethod = method.getAnnotation(MessageMethod.class);
            if (messageMethod != null) {
                if (!Arrays.deepEquals(method.getParameterTypes(), parameters)) {
                    throw new Exception("wrong parameters for @MessageMethod: " + method.toString());
                }
                if (!Result.class.isAssignableFrom(method.getReturnType())) {
                    throw new Exception("wrong return type for @MessageMethod: " + method.toString());
                }
                methods.add(method);
            }
        }

        return methods;
    }

    private Method getMessageMethodAnnotatedMethod(Class clazz, String name) throws Exception {
        for (Method method : getMessageMethodAnnotatedMethods(clazz)) {
            if (method.getName().equals(name)) {
                return method;
            }
        }

        return null;
    }
}
