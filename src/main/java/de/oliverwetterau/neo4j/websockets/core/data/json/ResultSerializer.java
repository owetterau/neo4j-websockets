package de.oliverwetterau.neo4j.websockets.core.data.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import de.oliverwetterau.neo4j.websockets.core.data.Result;

import java.io.IOException;

/**
 * Created by Oliver Wetterau on 05.02.15.
 */
public class ResultSerializer extends JsonSerializer<Result> {
    @Override
    public void serialize(Result result, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException
    {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeBooleanField("Ok", result.isOk());
        if (result.getErrors() != null) {
            jsonGenerator.writeObjectField("Errors", result.getErrors());
        }
        jsonGenerator.writeObjectField("Data", result.getData());

        Result<?> r = result;

        jsonGenerator.writeEndObject();
    }
}
