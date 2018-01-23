package objective.taskboard.jira.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class JiraLinkDto {
    @JsonProperty
    private LinkedIssueType type;
    
    @JsonProperty
    private LinkedIssue inwardIssue;
    
    @JsonProperty
    private LinkedIssue outwardIssue;
    
    private JiraIssueLinkTypeDto linkIssueType;

    public JiraIssueLinkTypeDto getIssueLinkType() {
        if (linkIssueType != null) return linkIssueType;
        JiraIssueLinkTypeDto.Direction direction = determineDirection();
        return linkIssueType = new JiraIssueLinkTypeDto(
                type.name, 
                direction == JiraIssueLinkTypeDto.Direction.INBOUND?type.inward: type.outward,
                direction);
    }

    public String getTargetIssueKey() {
        if (inwardIssue != null)
            return inwardIssue.key;
        return outwardIssue.key;
    }
    
    private JiraIssueLinkTypeDto.Direction determineDirection() {
        if (inwardIssue != null)
            return JiraIssueLinkTypeDto.Direction.INBOUND;
        
        return JiraIssueLinkTypeDto.Direction.OUTBOUND;
    }    

    @JsonIgnoreProperties(ignoreUnknown=true)
    public class LinkedIssue {
        public String key;
    }
    
    @JsonIgnoreProperties(ignoreUnknown=true)
    public class LinkedIssueType {
        public String name;
        public String inward;
        public String outward;
    }
}
