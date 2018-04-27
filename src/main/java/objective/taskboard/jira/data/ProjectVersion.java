package objective.taskboard.jira.data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import retrofit.http.GET;
import retrofit.http.Path;

public class ProjectVersion implements Serializable {
    private static final long serialVersionUID = 7468271426629759519L;

    public String id;
    public String name;
    public boolean archived;
    public boolean released;
    public LocalDate releaseDate;
    
    public interface Service {
        @GET("/rest/api/2/project/{projectKey}/versions")
        List<ProjectVersion> list(@Path("projectKey") String projectKey);
    }
}
