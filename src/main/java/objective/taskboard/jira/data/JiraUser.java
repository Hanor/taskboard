package objective.taskboard.jira.data;

import java.net.URI;
import java.util.List;
import java.util.Map;

import retrofit.http.GET;
import retrofit.http.Query;

public class JiraUser {

    public static String S24_24 = "24x24";
    public static String S48_48 = "48x48";

    public String name;
    public String displayName;
    public String emailAddress;
    public Map<String, URI> avatarUrls;
    public String locale;
    
    public JiraUser(){}

    public JiraUser(String name, String displayName, String emailAddress, Map<String, URI> avatarUrls) {
        this.name = name;
        this.displayName = displayName;
        this.emailAddress = emailAddress;
        this.avatarUrls = avatarUrls;
    }

    public URI getAvatarUri24() {
        return avatarUrls.get(S24_24);
    }

    public URI getAvatarUri48() {
        return avatarUrls.get(S48_48);
    }

    public static class JiraUserAutocomplete {
        public String name;
        public String displayName;
        public String emailAddress;
    }

    public interface Service {
        @GET("/rest/api/latest/user?expand=groups")
        JiraUser get(@Query("username") String name);

        @GET("/rest/api/latest/myself")
        JiraUser myself();

        @GET("/rest/projectbuilder/1.0/users")
        List<JiraUserAutocomplete> findUsers(@Query("q") String name);
    }
}
