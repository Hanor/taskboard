package objective.taskboard.jira.client;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import retrofit.http.GET;
import retrofit.http.Path;

@JsonIgnoreProperties(ignoreUnknown=true)
public class JiraWorklogResultSetDto {
    public int maxResults;
    public int startAt;
    public int total;
    public List<JiraWorklogDto> worklogs = new LinkedList<>();
    
    public interface Service {
        @GET("/rest/api/latest/issue/{key}/worklog")
        public JiraWorklogResultSetDto worklogsForIssue(@Path("key") String issueKey);
    }
}
