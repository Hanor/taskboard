package objective.taskboard.jira.data;

import com.google.common.collect.Maps;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.PUT;
import retrofit.http.Path;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

public class JiraIssue {

    public interface Service {
        @PUT("/rest/api/latest/issue/{id}")
        Response update(@Path("id") String id, @Body Update update);
    }

    public static class Update {

        public static UpdateBuilder builder() {
            return new UpdateBuilder();
        }

        public final Map<String, Object> fields;

        public Update(Map<String, Object> fields) {
            this.fields = fields;
        }
    }

    public static class UpdateBuilder {

        private UpdateBuilder() {}

        protected final Map<String, Object> fields = Maps.newLinkedHashMap();

        public FieldBuilder field(String field) {
            return new FieldBuilder(this, field);
        }

        public Update build() {
            return new Update(fields);
        }
    }

    public static class FieldBuilder {
        private final UpdateBuilder builder;
        private final String field;

        public FieldBuilder(UpdateBuilder builder, String field) {
            this.builder = builder;
            this.field = field;
        }

        public UpdateBuilder byName(String name) {
            builder.fields.put(field, singletonMap("name", name));
            return builder;
        }

        public UpdateBuilder byNames(String... names) {
            return byNames(asList(names));
        }

        public UpdateBuilder byNames(Collection<String> names) {
            builder.fields.put(field, names.stream()
                    .map(name -> singletonMap("name", name))
                    .collect(Collectors.toList()));
            return builder;
        }
    }
}
