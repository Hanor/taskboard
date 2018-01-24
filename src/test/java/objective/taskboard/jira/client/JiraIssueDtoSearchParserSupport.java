package objective.taskboard.jira.client;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JiraIssueDtoSearchParserSupport {

    public JiraIssueDtoSearch parse(String jsonObject) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonObject, JiraIssueDtoSearch.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
