package objective.taskboard.jira.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import retrofit.http.GET;
import retrofit.http.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueDto {
    public interface Service {
        @GET("/rest/api/latest/issue/{issueKey}?expand=schema,names,changelog")
        JiraIssueDto get(@Path("issueKey") String issueKey);
    }
    
    private ChangelogDto changelog;
    private String key;
    private Long id;
    
    @JsonProperty
    private JiraIssueDtoFields fields;
    
    @JsonProperty
    private Map<String, String> names;
    
    public String getKey() {
        return key;
    }
    
    public Long getId() {
        return id;
    }
    
    public List<ChangelogGroupDto> getChangelog() {
        return changelog.getHistories();
    }    

    public JiraUserDto getAssignee() {
        return fields.assignee;
    }

    public DateTime getCreationDate() {
        return fields.created;
    }

    public DateTime getUpdateDate() {
        return fields.updated;
    }

    public JiraProjectDto getProject() {
        return fields.project;
    }

    public JiraIssueTypeDto getIssueType() {
        return fields.issuetype;
    }

    public String getSummary() {
        return fields.summary;
    }

    public JiraStatusDto getStatus() {
        return fields.status;
    }

    public DateTime getDueDate() {
        return fields.dueDate;
    }

    public String getDescription() {
        return fields.description;
    }

    public JiraPriorityDto getPriority() {
        return fields.priority;
    }

    public List<JiraCommentDto> getComments() {
        return fields.comment;
    }

    public List<JiraLinkDto> getIssueLinks() {
        return fields.issuelinks;
    }

    public Set<String> getLabels() {
        return fields.labels;
    }

    public List<JiraComponentDto> getComponents() {
        return fields.components;
    }

    public JiraUserDto getReporter() {
        return fields.reporter;
    }

    public JiraTimeTrackingDto getTimeTracking() {
        return fields.timetracking;
    }

    public List<JiraSubtaskDto> getSubtasks() {
        return fields.subtasks;
    }
    
    public JiraWorklogResultSetDto getWorklogs() {
        if (fields.worklog == null)
            return new JiraWorklogResultSetDto();
        return fields.worklog;
    }
    
    public void setWorklogs(JiraWorklogResultSetDto worklogsForIssue) {
        this.fields.worklog = worklogsForIssue;
    }

    public JiraIssueFieldDto getField(String field) {
        JSONObjectAdapter jsonObjectAdapter = fields.other.get(field);
        if (jsonObjectAdapter == null)
            return null;
        String name = getFieldName(field);
        return new JiraIssueFieldDto(name, jsonObjectAdapter.object);
    }

    private String getFieldName(String field) {
        if (names == null)
            return "NAME NOT RESOLVED";
        
        return (String) ObjectUtils.defaultIfNull(names.get(field), "NAME NOT RESOLVED");
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class JiraIssueDtoFields {
        public JiraTimeTrackingDto timetracking;
        public JiraUserDto reporter;
        public List<JiraComponentDto> components = new ArrayList<>();
        public Set<String> labels = new HashSet<>();
        public List<JiraLinkDto> issuelinks = new ArrayList<>();
        public List<JiraCommentDto> comment = new ArrayList<>();
        public JiraPriorityDto priority;
        public String description;
        public List<JiraSubtaskDto> subtasks;
        
        @JsonDeserialize(using=JodaDateTimeDeserializer.class)
        public DateTime dueDate;
        
        public JiraStatusDto status;
        public String summary;
        public JiraIssueTypeDto issuetype;
        public JiraProjectDto project;
        
        @JsonDeserialize(using=JodaDateTimeDeserializer.class)
        public DateTime updated;
        
        @JsonDeserialize(using=JodaDateTimeDeserializer.class)
        public DateTime created;
        public JiraUserDto assignee;
        
        public JiraWorklogResultSetDto worklog;
        
        private Map<String, JSONObjectAdapter> other = new HashMap<>();

        @JsonAnyGetter
        public Map<String, JSONObjectAdapter> other() {
            return other;
        }
        
        @JsonAnySetter
        public void set(String name, JSONObjectAdapter value) {
            other.put(name, value);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(using = JSONObjectAdapterDeserializer.class)
    public static class JSONObjectAdapter {
        public Object object;
        public JSONObjectAdapter() {
            
        }

        public JSONObjectAdapter(Object jsonObjectValue) {
            this.object = jsonObjectValue;
        }
    }
    
    public static class JSONObjectAdapterDeserializer extends StdDeserializer<JSONObjectAdapter> {
        private static final long serialVersionUID = 1894757527363240771L;

        public JSONObjectAdapterDeserializer(){
            this(null);
        }
        
        protected JSONObjectAdapterDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public JSONObjectAdapter deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            switch (node.getNodeType()) {
            case OBJECT:
                try {
                    return new JSONObjectAdapter(new JSONObject(node.toString()));
                }catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            case ARRAY:
                try {
                    return new JSONObjectAdapter(new JSONArray(node.toString()));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            case BOOLEAN:
                return new JSONObjectAdapter(node.asBoolean());
            case NUMBER:
                return new JSONObjectAdapter(node.asDouble());
            case STRING:
                return new JSONObjectAdapter(node.asText());
            case BINARY:
            case MISSING:
            case POJO:
                throw new IllegalStateException("Unsupported type exception");
            case NULL:
            }
            return new JSONObjectAdapter();
        }
    }

    public void setFieldNames(Map<String, String> names) {
        this.names = names;
    }
}