package objective.taskboard.jira.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class JiraProjectDto {
    private String key;
    private String name;

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }
}
