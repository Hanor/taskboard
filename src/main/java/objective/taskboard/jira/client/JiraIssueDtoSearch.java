package objective.taskboard.jira.client;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import retrofit.http.Body;
import retrofit.http.POST;

@JsonIgnoreProperties(ignoreUnknown=true)
public class JiraIssueDtoSearch {
    public interface Service {
        @POST("/rest/api/latest/search")
        JiraIssueDtoSearch search(@Body Map<String, Object> input);
    }
    
    private List<JiraIssueDto> issues;
    private int maxResults;
    private int startAt;
    private int total;

    public List<JiraIssueDto> getIssues() {
        return issues;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public int getStartAt() {
        return startAt;
    }

    public int getTotal() {
        return total;
    }
}
