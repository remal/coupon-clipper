package app.utils;

import static com.fasterxml.jackson.core.JsonFactory.Feature.INTERN_FIELD_NAMES;
import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_JAVA_COMMENTS;
import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_SINGLE_QUOTES;
import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_TRAILING_COMMA;
import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS;
import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.Instantiatable;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public abstract class Json {

    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
        .disable(INTERN_FIELD_NAMES)
        .enable(ALLOW_JAVA_COMMENTS)
        .enable(ALLOW_SINGLE_QUOTES)
        .enable(ALLOW_UNQUOTED_FIELD_NAMES)
        .enable(ALLOW_TRAILING_COMMA)
        .build();

    public static final JsonMapper JSON_MAPPER = JsonMapper.builder(JSON_FACTORY)
        .disable(WRITE_DATES_AS_TIMESTAMPS)
        .disable(WRITE_DATE_KEYS_AS_TIMESTAMPS)
        .enable(FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(INDENT_OUTPUT)
        .enable(SORT_PROPERTIES_ALPHABETICALLY)
        .defaultPrettyPrinter(new CustomJSONPrettyPrinter())
        .findAndAddModules()
        .build();


    private static class CustomJSONPrettyPrinter
        implements PrettyPrinter, Instantiatable<CustomJSONPrettyPrinter>, Serializable {

        @Serial
        private static final long serialVersionUID = 1;

        @Override
        public CustomJSONPrettyPrinter createInstance() {
            return new CustomJSONPrettyPrinter();
        }

        private static final String INDENT = "  ";

        private transient int depth = 0;

        private void writeIndent(JsonGenerator gen) throws IOException {
            for (int i = 0; i < depth; ++i) {
                gen.writeRaw(INDENT);
            }
        }

        @Override
        public void writeRootValueSeparator(JsonGenerator gen) throws IOException {
            gen.writeRaw("\n");
        }

        @Override
        public void writeStartObject(JsonGenerator gen) throws IOException {
            gen.writeRaw("{");
            ++depth;
        }

        @Override
        public void beforeObjectEntries(JsonGenerator gen) throws IOException {
            gen.writeRaw("\n");
            writeIndent(gen);
        }

        @Override
        public void writeObjectFieldValueSeparator(JsonGenerator gen) throws IOException {
            gen.writeRaw(": ");
        }

        @Override
        public void writeObjectEntrySeparator(JsonGenerator gen) throws IOException {
            gen.writeRaw(",\n");
            writeIndent(gen);
        }

        @Override
        public void writeEndObject(JsonGenerator gen, int nrOfEntries) throws IOException {
            --depth;
            if (1 <= nrOfEntries) {
                gen.writeRaw("\n");
                writeIndent(gen);
            }
            gen.writeRaw("}");
        }

        @Override
        public void writeStartArray(JsonGenerator gen) throws IOException {
            gen.writeRaw("[");
            ++depth;
        }

        @Override
        public void beforeArrayValues(JsonGenerator gen) throws IOException {
            gen.writeRaw("\n");
            writeIndent(gen);
        }

        @Override
        public void writeArrayValueSeparator(JsonGenerator gen) throws IOException {
            gen.writeRaw(",\n");
            writeIndent(gen);
        }

        @Override
        public void writeEndArray(JsonGenerator gen, int nrOfValues) throws IOException {
            --depth;
            if (1 <= nrOfValues) {
                gen.writeRaw("\n");
                writeIndent(gen);
            }
            gen.writeRaw("]");
        }

    }


}
