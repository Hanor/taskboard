package objective.taskboard.jira.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class JiraStatusDto {
    private long id;

    public long getId() {
        return id;
    }
}
