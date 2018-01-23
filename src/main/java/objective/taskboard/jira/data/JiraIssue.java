package objective.taskboard.jira.data;

import com.google.common.collect.Maps;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

public class JiraIssue {

    public String key;
    
    public JiraIssue(){}

    public JiraIssue(String key) {
        this.key = key;
    }

    public interface Service {
        @POST("/rest/api/latest/issue")
        JiraIssue create(@Body Input input);

        @PUT("/rest/api/latest/issue/{id}")
        Response update(@Path("id") String id, @Body Input input);
    }

    public static class Input {

        public static InputBuilder builder() {
            return new InputBuilder();
        }

        public final String summary;
        public final Map<String, Object> fields;

        public Input(String summary, Map<String, Object> fields) {
            this.summary = summary;
            this.fields = fields;
        }
    }

    public static class InputBuilder {

        private InputBuilder() {}

        protected String summary;
        protected final Map<String, Object> fields = Maps.newLinkedHashMap();

        public InputBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public FieldBuilder field(String field) {
            return new FieldBuilder(this, field);
        }

        public Input build() {
            return new Input(summary, fields);
        }
    }

    public static class FieldBuilder {
        private final InputBuilder builder;
        private final String field;

        public FieldBuilder(InputBuilder builder, String field) {
            this.builder = builder;
            this.field = field;
        }

        public InputBuilder byId(long id) {
            return byId(Long.toString(id));
        }

        public InputBuilder byId(String id) {
            builder.fields.put(field, singletonMap("id", id));
            return builder;
        }

        public InputBuilder byKey(String key) {
            builder.fields.put(field, singletonMap("key", key));
            return builder;
        }

        public InputBuilder byName(String name) {
            builder.fields.put(field, singletonMap("name", name));
            return builder;
        }

        public InputBuilder byNames(String... names) {
            return byNames(asList(names));
        }

        public InputBuilder byNames(Collection<String> names) {
            builder.fields.put(field, names.stream()
                    .map(name -> singletonMap("name", name))
                    .collect(Collectors.toList()));
            return builder;
        }

        public InputBuilder value(Object value) {
            builder.fields.put(field, value);
            return builder;
        }
    }
}
