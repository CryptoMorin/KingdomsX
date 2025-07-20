package org.kingdoms.utils.gson;

import com.cryptomorin.xseries.reflection.XReflection;
import com.google.gson.*;
import com.google.gson.internal.GsonBuildConfig;
import com.google.gson.internal.Streams;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;

import java.io.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Objects;

public final class KingdomsGson {
    private static final boolean SUPPORTS_JsonObject_size = XReflection.of(JsonObject.class)
            .method("public int size()").exists();

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting().disableHtmlEscaping().enableComplexMapKeySerialization()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
            .create();

    private static final class GsonContextImpl implements JsonSerializationContext, JsonDeserializationContext {
        @Override
        public JsonElement serialize(Object src) {
            return KingdomsGson.GSON.toJsonTree(src);
        }

        @Override
        public JsonElement serialize(Object src, Type typeOfSrc) {
            return KingdomsGson.GSON.toJsonTree(src, typeOfSrc);
        }

        @Override
        public <R> R deserialize(JsonElement json, Type typeOfT) throws JsonParseException {
            return KingdomsGson.GSON.fromJson(json, typeOfT);
        }
    }

    private static final GsonContextImpl CONTEXT = new GsonContextImpl();

    public static int size(JsonObject object) {
        if (SUPPORTS_JsonObject_size) return object.size();
        return object.entrySet().size(); // entrySet initialize extra objects so it's not preferred.
    }

    private static JsonReader newJsonReader(Reader reader) {
        return new JsonReader(reader);
    }

    public static JsonElement fromObject(Object object) {
        if (object instanceof JsonElement) return (JsonElement) object;
        return CONTEXT.serialize(object);
    }

    public static JsonElement fromString(String json) {
        // JsonParser.parseString(json) doesn't exist for older versions of GSON.
        try {
            JsonReader jsonReader = new JsonReader(new StringReader(json));
            JsonElement element = parseReader(jsonReader);
            if (!element.isJsonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonSyntaxException("Did not consume the entire document.");
            } else {
                return element;
            }
        } catch (MalformedJsonException | NumberFormatException ex) {
            throw new JsonSyntaxException(ex);
        } catch (IOException ex) {
            throw new JsonIOException(ex);
        }
    }

    private static JsonElement parseReader(JsonReader reader) throws JsonIOException, JsonSyntaxException {
        try {
            return Streams.parse(reader);
        } catch (StackOverflowError | OutOfMemoryError ex) {
            throw new JsonParseException("Failed parsing JSON source: " + reader + " to Json", ex);
        }
    }

    public static String toString(JsonElement element) {
        Objects.requireNonNull(element);
        try {
            StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            jsonWriter.setLenient(false);
            jsonWriter.setSerializeNulls(false);
            jsonWriter.setHtmlSafe(false);
            Streams.write(element, jsonWriter);
            return stringWriter.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static JsonWriter newJsonWriter(Writer writer) throws IOException {
        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.setIndent("  ");
        jsonWriter.setLenient(false);
        jsonWriter.setSerializeNulls(false);
        jsonWriter.setHtmlSafe(false);
        return jsonWriter;
    }

    public static String toJson(Object src) {
        return KingdomsGson.GSON.toJson(src);
    }

    public static void toJson(JsonObject object, Appendable writer) throws IOException {
        JsonWriter jsonWriter = newJsonWriter(Streams.writerForAppendable(writer));
        Streams.write(object, jsonWriter);
    }

    private static void assertFullConsumption(Object obj, JsonReader reader) {
        try {
            if (obj != null && reader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonIOException("JSON document was not fully consumed.");
            }
        } catch (MalformedJsonException e) {
            throw new JsonSyntaxException(e);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }

    public static JsonObject parse(Reader json) throws JsonIOException, JsonSyntaxException {
        JsonReader reader = newJsonReader(json);
        JsonObject object;
        reader.setLenient(false);
        try {
            reader.peek();
            object = (JsonObject) Streams.parse(reader);
        } catch (IllegalStateException e) {
            throw new JsonSyntaxException(e);
        } catch (IOException e) {
            // TODO(inder): Figure out whether it is indeed right to rethrow this as JsonSyntaxException
            throw new JsonSyntaxException(e);
        } catch (AssertionError e) {
            throw new AssertionError("AssertionError (GSON " + GsonBuildConfig.VERSION + "): " + e.getMessage(), e);
        }
        assertFullConsumption(object, reader);
        return object;
    }

    private static JsonElement parse(JsonReader reader) throws JsonParseException {
        boolean isEmpty = true;
        try {
            reader.peek();
            isEmpty = false;
            return TypeAdapters.JSON_ELEMENT.read(reader);
        } catch (EOFException e) {
            /*
             * For compatibility with JSON 1.5 and earlier, we return a JsonNull for
             * empty documents instead of throwing.
             */
            if (isEmpty) {
                return JsonNull.INSTANCE;
            }
            // The stream ended prematurely so it is likely a syntax error.
            throw new JsonSyntaxException(e);
        } catch (MalformedJsonException e) {
            throw new JsonSyntaxException(e);
        } catch (IOException e) {
            throw new JsonIOException(e);
        } catch (NumberFormatException e) {
            throw new JsonSyntaxException(e);
        }
    }
}
