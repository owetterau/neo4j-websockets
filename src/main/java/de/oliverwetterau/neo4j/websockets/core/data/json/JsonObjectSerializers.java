package de.oliverwetterau.neo4j.websockets.core.data.json;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;

import java.util.Map;

/**
 * Created by oliver on 28.08.15.
 */
public interface JsonObjectSerializers {
    Map<Class,JsonSerializer> getSerializers();
    Map<Class,JsonDeserializer> getDeserializers();
}
