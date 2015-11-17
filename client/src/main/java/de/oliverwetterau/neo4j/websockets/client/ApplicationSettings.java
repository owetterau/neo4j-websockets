package de.oliverwetterau.neo4j.websockets.client;

/**
 * Created by oliver on 16.11.15.
 */
public class ApplicationSettings {
    private static String managementPath = "/ws/management";
    private static String dataPath = "/ws/data";
    private static String[] serverURIs = new String[0];
    private static boolean binaryCommunication = true;

    public static String managementPath() {
        return managementPath;
    }

    public static void setManagementPath(String path) {
        managementPath = sanitizePath(path);
    }

    public static String dataPath() {
        return dataPath;
    }

    public static void setDataPath(String path) {
        dataPath = sanitizePath(path);
    }

    public static String[] serverURIs() {
        return serverURIs;
    }

    public static void setServerURIs(String[] URIs) {
        serverURIs = URIs;
    }

    public static boolean binaryCommunication() {
        return binaryCommunication;
    }

    public static void setBinaryCommunication(boolean binary) {
        binaryCommunication = binary;
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
}
