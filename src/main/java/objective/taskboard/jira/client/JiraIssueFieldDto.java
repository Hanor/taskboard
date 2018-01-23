package objective.taskboard.jira.client;

public class JiraIssueFieldDto {
    private String name;
    private Object value;

    public JiraIssueFieldDto(String name, Object value) {
        this.name = name;
        this.value = value;
        
    }
    public JiraIssueFieldDto(Object issueFieldValue) {
        this.value = issueFieldValue;
    }
    
    public String getName() {
        return name;
    }    

    public Object getValue() {
        return value;
    }
}
