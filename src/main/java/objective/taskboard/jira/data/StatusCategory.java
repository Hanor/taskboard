package objective.taskboard.jira.data;


import java.util.List;

import retrofit.http.GET;

public class StatusCategory {

    public Long id;
    public String key;
    public String name;
    public String colorName;
    
    public StatusCategory(){}

    public StatusCategory(Long id, String key, String name, String colorName) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.colorName = colorName;
    }

    public interface Service {
        @GET("/rest/api/latest/statuscategory")
        List<StatusCategory> all();
    }
}
