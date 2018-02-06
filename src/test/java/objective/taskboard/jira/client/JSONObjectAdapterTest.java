package objective.taskboard.jira.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JSONObjectAdapterTest {

    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    }

    @Test
    public void serializeNull() throws JsonProcessingException {
        JiraIssueDto.JSONObjectAdapter n = new JiraIssueDto.JSONObjectAdapter();
        assertThat(objectMapper.writeValueAsString(n)).isEqualTo("null");
    }

    @Test
    public void serializeBoolean() throws JsonProcessingException {
        JiraIssueDto.JSONObjectAdapter b = new JiraIssueDto.JSONObjectAdapter(true);
        assertThat(objectMapper.writeValueAsString(b)).isEqualTo("true");
    }

    @Test
    public void serializeDouble() throws JsonProcessingException {
        JiraIssueDto.JSONObjectAdapter d = new JiraIssueDto.JSONObjectAdapter(1.25);
        assertThat(objectMapper.writeValueAsString(d)).isEqualTo("1.25");
    }

    @Test
    public void serializeString() throws JsonProcessingException {
        JiraIssueDto.JSONObjectAdapter s = new JiraIssueDto.JSONObjectAdapter("test");
        assertThat(objectMapper.writeValueAsString(s)).isEqualTo("\"test\"");
    }

    @Test
    public void serializeJsonObject() throws JSONException, JsonProcessingException {
        JiraIssueDto.JSONObjectAdapter o = new JiraIssueDto.JSONObjectAdapter(new JSONObject("{\"hello\": \"world\"}"));
        assertThat(objectMapper.writeValueAsString(o)).isEqualTo("{\"hello\":\"world\"}");
    }

    @Test
    public void serializeJsonArray() throws JSONException, JsonProcessingException {
        JiraIssueDto.JSONObjectAdapter a = new JiraIssueDto.JSONObjectAdapter(new JSONArray("[\"test\", 1.0, true]"));
        assertThat(objectMapper.writeValueAsString(a)).isEqualTo("[\"test\",1,true]");
    }

    @Test
    public void serializeObject() throws JSONException, JsonProcessingException {
        Properties object = new Properties();
        object.n = new JiraIssueDto.JSONObjectAdapter();
        object.b = new JiraIssueDto.JSONObjectAdapter(true);
        object.d = new JiraIssueDto.JSONObjectAdapter(1.25);
        object.s = new JiraIssueDto.JSONObjectAdapter("test");
        object.o = new JiraIssueDto.JSONObjectAdapter(new JSONObject("{\"hello\": \"world\"}"));
        object.a = new JiraIssueDto.JSONObjectAdapter(new JSONArray("[\"test\", 1.0, true]"));
        assertThat(objectMapper.writeValueAsString(object))
                .startsWith("{")
                .contains("\"n\":null")
                .contains("\"b\":true")
                .contains("\"d\":1.25")
                .contains("\"s\":\"test\"")
                .contains("\"o\":{\"hello\":\"world\"}")
                .contains("\"a\":[\"test\",1,true]")
                .endsWith("}");
    }

    @Test
    public void deserializeObject() throws IOException {
        String json = "{"
                + "\"n\":null"
                + ",\"b\":true"
                + ",\"d\":1.25"
                + ",\"s\":\"test\""
                + ",\"o\":{\"hello\":\"world\"}"
                + ",\"a\":[\"test\",1,true]"
                + "}";

        Properties object = objectMapper.readValue(json, Properties.class);
        assertThat(object.n).isNull();
        assertThat(object.b.object).isEqualTo(true);
        assertThat(object.d.object).isEqualTo(1.25);
        assertThat(object.s.object).isEqualTo("test");
        assertThat(object.o.object).isInstanceOf(JSONObject.class);
        assertThat(object.a.object).isInstanceOf(JSONArray.class);
    }

    public static class Properties {
        @JsonProperty
        JiraIssueDto.JSONObjectAdapter n;
        @JsonProperty
        JiraIssueDto.JSONObjectAdapter b;
        @JsonProperty
        JiraIssueDto.JSONObjectAdapter d;
        @JsonProperty
        JiraIssueDto.JSONObjectAdapter s;
        @JsonProperty
        JiraIssueDto.JSONObjectAdapter o;
        @JsonProperty
        JiraIssueDto.JSONObjectAdapter a;
    }
}