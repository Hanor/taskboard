package objective.taskboard.jira.client;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class JiraWorklogDto {
    public JiraUserDto author;
    public Date started;
    public int timeSpentSeconds;
    public int issueId;
}