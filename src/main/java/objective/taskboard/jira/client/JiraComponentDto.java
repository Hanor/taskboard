package objective.taskboard.jira.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class JiraComponentDto {
    private String name;
    
    public String getName() {
        return name;
    }
}
