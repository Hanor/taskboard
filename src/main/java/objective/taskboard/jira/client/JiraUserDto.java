package objective.taskboard.jira.client;

import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class JiraUserDto {

    private String self;
    private String name;
    private String displayName;
    
    @JsonProperty
    private Map<String, URI> avatarUrls;

    public URI getAvatarUri(String iconSize) {
        return avatarUrls.get(iconSize);
    }

    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getSelf() {
        return self;
    }
}
