package objective.taskboard.jira;

import objective.taskboard.jira.client.JiraIssueDto;

public interface SearchIssueVisitor {
    public void processIssue(JiraIssueDto issue);

    default void complete(){}
}
