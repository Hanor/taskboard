package objective.taskboard.jira.data;

import java.net.URI;
import java.util.Map;

import retrofit.http.GET;
import retrofit.http.Query;

public class JiraUser {

    public static String S48_48 = "48x48";

    public String name;
    public String displayName;
    public String emailAddress;
    public Map<String, URI> avatarUrls;
    
    public JiraUser(){}

    public JiraUser(String name, String displayName, String emailAddress, Map<String, URI> avatarUrls) {
        this.name = name;
        this.displayName = displayName;
        this.emailAddress = emailAddress;
        this.avatarUrls = avatarUrls;
    }

    public URI getAvatarUri() {
        return avatarUrls.get(S48_48);
    }

    public interface Service {
        @GET("/rest/api/latest/user?expand=groups")
        JiraUser get(@Query("username") String name);
    }
}
