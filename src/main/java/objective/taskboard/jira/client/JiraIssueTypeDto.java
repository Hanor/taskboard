package objective.taskboard.jira.client;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class JiraIssueTypeDto {

    private Long id;
    @JsonProperty
    private URI iconUrl;
    private String name;

    public Long getId() {
        return id;
    }

    public URI getIconUri() {
        return iconUrl;
    }
    
    public String getName() {
        return name;
    }

    public boolean isSubtask() {
        throw new RuntimeException("NOT IMPLEMENTED");
    }

    

}
