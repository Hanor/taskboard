package objective.taskboard.jira.data;

import retrofit.http.GET;

public class JiraTimezone {
    public final String timeZone;
    
    public JiraTimezone(String timeZone) {
        this.timeZone = timeZone;
    }
    
    public interface Service {
        @GET("/rest/api/latest/myself")
        JiraTimezone get();
    }
}
