package objective.taskboard.jira.client;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

public class JiraSearchTestSupport {

    public JiraIssueDtoSearch parse(String jsonObject) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return mapper.readValue(jsonObject, JiraIssueDtoSearch.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
