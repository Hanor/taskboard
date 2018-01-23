package objective.taskboard.jira.data;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Path;

public class JiraProject {

    public String id;
    public String key;
    public List<Version> versions;
    public String name;

    public JiraProject() {}
    public JiraProject(String id, String key, List<Version> versions, String name) {
        this.id = id;
        this.key = key;
        this.versions = versions;
        this.name = name;
    }

    public interface Service {
        @GET("/rest/api/latest/project")
        List<JiraProject> all();

        @GET("/rest/api/latest/project/{id}")
        JiraProject get(@Path("id") String id);
    }
}