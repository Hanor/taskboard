package objective.taskboard.jira.endpoint;

import org.springframework.beans.factory.annotation.Autowired;

import objective.taskboard.jira.endpoint.JiraEndpoint.Request;

public abstract class AuthorizedJiraEndpoint {
    
    @Autowired
    private JiraEndpoint jiraEndpoint;

    public <T> T executeRequest(Request<T> request) {
        return jiraEndpoint.executeRequest(getUsername(), getPassword(), request);
    }

    public <S> S request(Class<S> service) {
        return jiraEndpoint.request(service, getUsername(), getPassword());
    }
    
    protected abstract String getUsername();
    
    protected abstract String getPassword();
}
