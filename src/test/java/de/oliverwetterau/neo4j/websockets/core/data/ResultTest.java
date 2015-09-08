package de.oliverwetterau.neo4j.websockets.core.data;

import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by oliver on 28.08.15.
 */
public class ResultTest {
    private static List<Error> errors;

    @BeforeClass
    public static void setup() {
        JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(null);

        Result.setJsonObjectMapper(jsonObjectMapper);
        Error.setJsonObjectMapper(jsonObjectMapper);

        errors = new ArrayList<>();
        errors.add(new Error("Type One", "Message One"));
        errors.add(new Error("Type Two", "Message Two"));
        errors.add(new Error("Type Three", "Message Three"));
    }

    public void testResultData(Result result, boolean isStringDirty, boolean isBytesDirty, int errorCount, int dataCount)
            throws Exception
    {
        assertEquals("string value should be dirty", isStringDirty, result.isStringDirty);
        assertEquals("string value should be dirty", isBytesDirty, result.isBytesDirty);

        if (errorCount == 0) {
            assertNull("error list should be null", result.errors);
        }
        else {
            assertEquals("error list should contain " + errorCount + " element(s)", errorCount, result.errors.size());
        }
        assertEquals("data list should contain " + dataCount + " element(s)", dataCount, result.data.size());
    }

    public void testResultData(Result result, int errorCount, int dataCount)
            throws Exception
    {
        testResultData(result, true, true, errorCount, dataCount);
    }

    @Test
    public void testSetJsonObjectMapper() throws Exception {
        JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(null);

        Result.setJsonObjectMapper(jsonObjectMapper);

        assertEquals(jsonObjectMapper, Result.jsonObjectMapper);
    }

    @Test
    public void testConstructor() throws Exception {
        Result result = new Result();
        testResultData(result, 0, 0);
    }

    @Test
    public void testConstructorClass() throws Exception {
        Result<String> stringResult = new Result<>(String.class);
        testResultData(stringResult, 0, 0);
    }

    @Test
    public void testConstructorSize() throws Exception {
        Result result = new Result(10);
        testResultData(result, 0, 0);
    }

    @Test
    public void testConstructorObject() throws Exception {
        Result<String> stringResult = new Result<>("Test");
        testResultData(stringResult, 0, 1);
        assertEquals("Test", stringResult.getData().get(0));
    }

    @Test
    public void testConstructorError() throws Exception {
        Error error = new Error("type", "message");
        Result<String> stringResult = new Result<>(error);
        testResultData(stringResult, 1, 0);
        assertEquals(error, stringResult.errors.get(0));
    }

    @Test
    public void testConstructorErrorList() throws Exception {
        List<Error> errorList = new ArrayList<>();
        errorList.addAll(errors);

        Result<String> stringResult = new Result<>(errorList);

        testResultData(stringResult, 3, 0);
        assertTrue(stringResult.errors.equals(errorList));

        errorList.add(new Error("Type", "Message"));

        testResultData(stringResult, 3, 0);
        assertFalse(stringResult.errors.equals(errorList));
    }

    @Test
    public void testAddError() throws Exception {
        Error error = new Error("Type", "Message");
        Result result = new Result();

        result.isStringDirty = false;
        result.isBytesDirty = false;

        result.add(error);

        testResultData(result, 1, 0);
        assertEquals(error, result.errors.get(0));
    }

    @Test
    public void testAddErrors() throws Exception {

    }

    @Test
    public void testGetErrors() throws Exception {

    }

    @Test
    public void testErrorsToHtml() throws Exception {

    }

    @Test
    public void testAdd1() throws Exception {

    }

    @Test
    public void testAdd2() throws Exception {

    }

    @Test
    public void testGetData() throws Exception {

    }

    @Test
    public void testGetSingleData() throws Exception {

    }

    @Test
    public void testIsOk() throws Exception {

    }

    @Test
    public void testToJsonString() throws Exception {

    }

    @Test
    public void testToJsonBytes() throws Exception {

    }
}
