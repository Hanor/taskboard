package objective.taskboard.jira.client;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JiraSubtaskDto {

    @JsonProperty
    private String key;
    @JsonProperty
    private JiraSubtaskDtoFields fields;

    public JiraIssueTypeDto getIssueType() {
        return fields.issuetype;
    }

    public String getIssueKey() {
        return key;
    }

    private static class JiraSubtaskDtoFields {
        public JiraIssueTypeDto issuetype;

        private Map<String, JiraIssueDto.JSONObjectAdapter> other = new HashMap<>();

        @JsonAnyGetter
        public Map<String, JiraIssueDto.JSONObjectAdapter> other() {
            return other;
        }

        @JsonAnySetter
        public void set(String name, JiraIssueDto.JSONObjectAdapter value) {
            other.put(name, value);
        }
    }
}
