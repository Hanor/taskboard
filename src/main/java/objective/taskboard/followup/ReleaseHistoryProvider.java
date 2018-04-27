package objective.taskboard.followup;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import objective.taskboard.jira.data.ProjectVersion;
import objective.taskboard.jira.endpoint.JiraEndpointAsMaster;

@Component
class ReleaseHistoryProvider {
    
    private JiraEndpointAsMaster jiraEndpoint;
    
    @Autowired
    public ReleaseHistoryProvider(JiraEndpointAsMaster jiraEndpoint) {
        this.jiraEndpoint = jiraEndpoint;
    }

    public List<ProjectRelease> get(String projectKey) {
        List<ProjectVersion> versions = jiraEndpoint.request(ProjectVersion.Service.class).list(projectKey);
        
        return versions.stream()
                .filter(v -> !v.archived && v.releaseDate != null)
                .map(v -> new ProjectRelease(v.name, v.releaseDate))
                .sorted(comparing(ProjectRelease::getDate))
                .collect(toList());
    }
    
    static class ProjectRelease {
        private final String name;
        private final LocalDate date;

        public ProjectRelease(String name, LocalDate date) {
            this.name = name;
            this.date = date;
        }
        
        public String getName() {
            return name;
        }
        
        public LocalDate getDate() {
            return date;
        }
    }
}
