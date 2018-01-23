package objective.taskboard.jira.client;

public class JiraSubtaskDto {

    private JiraIssueTypeDto issuetype;
    private String key;

    public JiraIssueTypeDto getIssueType() {
        return issuetype;
    }

    public String getIssueKey() {
        return key;
    }

}
