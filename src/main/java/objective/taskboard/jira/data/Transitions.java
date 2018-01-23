package objective.taskboard.jira.data;

import static objective.taskboard.jira.IssueFieldsUpdateSchema.makeUpdateSchema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public class Transitions {

    public transient String expand;
    public List<Transition> transitions;

    public Transitions(){}
    public Transitions(String expand, List<Transition> transitions) {
        this.expand = expand;
        this.transitions = transitions;
    }

    public interface Service {
        @GET("/rest/api/latest/issue/{issueKey}/transitions?expand=transitions.fields")
        Transitions get(@Path("issueKey") String issueKey);

        @POST("/rest/api/latest/issue/{issueKey}/transitions?expand=transitions.fields")
        Response doTransition(@Path("issueKey") String issueKey, @Body DoTransitionRequestBody transition);
    }

    public static class DoTransitionRequestBody {
        @JsonProperty
        private final Map<String, Object> transition = new HashMap<>();
        @JsonProperty
        private final Map<String, Object> update = new HashMap<>();

        public DoTransitionRequestBody(Long transitionId) {
            setTransitionOnRequest(transitionId);
        }

        public DoTransitionRequestBody(Long transitionId, Map<String, Object> fields) {
            setTransitionOnRequest(transitionId);
            if (fields != null && !fields.isEmpty())
                setFieldsOnTransitionSchema(fields);
        }

        private void setTransitionOnRequest(Long transitionId) {
            this.transition.put("id", transitionId);
        }

        private void setFieldsOnTransitionSchema(Map<String, Object> fields) {
            this.update.putAll(makeUpdateSchema(fields));
        }
    }

}
