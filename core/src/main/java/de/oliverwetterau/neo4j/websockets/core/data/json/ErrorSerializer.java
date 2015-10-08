package de.oliverwetterau.neo4j.websockets.core.data.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import de.oliverwetterau.neo4j.websockets.core.data.Error;

import java.io.IOException;

/**
 * Created by Oliver Wetterau on 05.02.15.
 */
public class ErrorSerializer extends JsonSerializer<Error> {
    @Override
    public void serialize(Error error, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException
    {
        jsonGenerator.writeObject(error.toJson());
    }
}
