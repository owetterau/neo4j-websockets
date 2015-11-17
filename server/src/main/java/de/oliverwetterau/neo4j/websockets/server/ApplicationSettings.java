package de.oliverwetterau.neo4j.websockets.server;

import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectSerializers;
import de.oliverwetterau.neo4j.websockets.core.i18n.ThreadLocale;
import de.oliverwetterau.neo4j.websockets.server.annotations.StartListener;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.List;

/**
 * Created by oliver on 11.11.15.
 */
public class ApplicationSettings {
    private static List<String> packageNames = null;
    private static String host = null;
    private static Integer port = null;
    private static String managementPath = null;
    private static String dataPath = null;

    private static Class jsonObjectSerializersClass = null;
    private static Class threadLocaleClass = null;
    private static Class startListenerClass = null;

    private ApplicationSettings() {
    }

    public static void configure(List<String> packageNames, String host, Integer port, String managementPath, String dataPath)
            throws Exception
    {
        ApplicationSettings.packageNames = packageNames;
        ApplicationSettings.host = host;
        ApplicationSettings.port = port;
        ApplicationSettings.managementPath = sanitizePath(managementPath);
        ApplicationSettings.dataPath = sanitizePath(dataPath);

        findInterfaceImplentations();
    }

    private static void findInterfaceImplentations() throws Exception {
        Class jsonObjectSerializersClass = null;
        Class readJsonObjectSerializersClass = null;
        Class threadLocaleClass = null;
        Class readThreadLocaleClass;
        Class startListenerClass = null;
        Class readStartListenerClass;

        for (String packageName : ApplicationSettings.packageNames()) {
            readJsonObjectSerializersClass = getJsonObjectSerializersClass(packageName);
            if (jsonObjectSerializersClass != null && readJsonObjectSerializersClass != null) {
                throw new Exception("More than one class implementing JsonObjectSerializers interface");
            }
            jsonObjectSerializersClass = readJsonObjectSerializersClass;

            readThreadLocaleClass = getThreadLocaleClass(packageName);
            if (threadLocaleClass != null && readThreadLocaleClass != null) {
                throw new Exception("More than one class implementing ThreadLocale interface");
            }
            threadLocaleClass = readThreadLocaleClass;

            readStartListenerClass = getStartListenerClass(packageName);
            if (startListenerClass != null && readStartListenerClass != null) {
                throw new Exception("More than one class implementing StartListener interface");
            }
            startListenerClass = readStartListenerClass;
        }

        ApplicationSettings.jsonObjectSerializersClass = readJsonObjectSerializersClass;
        ApplicationSettings.threadLocaleClass = threadLocaleClass;
        ApplicationSettings.startListenerClass = startListenerClass;
    }

    public static List<String> packageNames() {
        return packageNames;
    }

    public static String host() {
        return host;
    }

    public static Integer port() {
        return port;
    }

    public static String managementPath() {
        return managementPath;
    }

    public static String dataPath() {
        return dataPath;
    }

    public static Class jsonObjectSerializersClass() {
        return jsonObjectSerializersClass;
    }

    public static Class threadLocaleClass() {
        return threadLocaleClass;
    }

    public static Class startListenerClass() {
        return startListenerClass;
    }

    private static String sanitizePath(String path) {
        StringBuilder pathBuilder = new StringBuilder();

        if (!path.startsWith("/")) {
            pathBuilder.append("/");
        }
        if (path.endsWith("/")) {
            pathBuilder.append(path.substring(1, path.length() - 1));
        }
        else {
            pathBuilder.append(path);
        }

        return pathBuilder.toString();
    }

    private static Class getJsonObjectSerializersClass(String packageName) throws Exception {
        Class clazz = null;

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(JsonObjectSerializers.class));
        for (final BeanDefinition candidate : scanner.findCandidateComponents(packageName)) {
            if (jsonObjectSerializersClass != null || clazz != null) {
                throw new Exception("More than one class implementing JsonObjectSerializers interface");
            }
            clazz = ClassUtils.resolveClassName(candidate.getBeanClassName(), ClassUtils.getDefaultClassLoader());
        }

        return clazz;
    }

    private static Class getThreadLocaleClass(String packageName) throws Exception {
        Class clazz = null;

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(ThreadLocale.class));
        for (final BeanDefinition candidate : scanner.findCandidateComponents(packageName)) {
            if (threadLocaleClass != null || clazz != null) {
                throw new Exception("More than one class implementing ThreadLocale interface");
            }
            clazz = ClassUtils.resolveClassName(candidate.getBeanClassName(), ClassUtils.getDefaultClassLoader());
        }

        return clazz;
    }

    private static Class getStartListenerClass(String packageName) throws Exception {
        Class clazz = null;

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(StartListener.class));
        for (final BeanDefinition candidate : scanner.findCandidateComponents(packageName)) {
            if (startListenerClass != null || clazz != null) {
                throw new Exception("More than one class implementing StartListener interface");
            }
            clazz = ClassUtils.resolveClassName(candidate.getBeanClassName(), ClassUtils.getDefaultClassLoader());
        }

        return clazz;
    }
}
