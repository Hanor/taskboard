package objective.taskboard.jira.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import retrofit.http.Body;
import retrofit.http.POST;

import java.io.Serializable;

public class Version implements Serializable {

    private static final long serialVersionUID = -845658400746750858L;

    public final String id;
    public final String name;

    @JsonCreator
    public Version(@JsonProperty("id") String id, @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }

    public interface Service {
        @POST("/rest/api/latest/version")
        Version create(@Body Version.Request request);
    }

    public static class Request {

        public String project;
        public String name;
        
        public Request(){}

        public Request(String project, String name) {
            this.project = project;
            this.name = name;
        }
    }
}
