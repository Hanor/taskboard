package objective.taskboard.jira;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.issueBuffer.CardRepo;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;

@Service
public class JiraIssueJqlBuilderService {
    @Autowired
    private ProjectFilterConfigurationCachedRepository projectRepository;
    
    @Autowired
    private MetadataCachedService metadataService;
    
    public String projectsJql(CardRepo cardsRepo) {
        String jql = projectsSqlWithoutTimeConstraint();
        return addTimeConstraintIfPresent(cardsRepo.getLastUpdatedDate(), jql);
    }

    public String projectsSqlWithoutTimeConstraint() {
        List<ProjectFilterConfiguration> projects = projectRepository.getProjects();
        String projectKeys = "'" + projects.stream()
                                           .map(ProjectFilterConfiguration::getProjectKey)
                                           .collect(Collectors.joining("','")) + "'";
        String jql = String.format("project in (%s) ", projectKeys);
        return jql;
    }

    private String addTimeConstraintIfPresent(Optional<Date> lastRemoteUpdatedDate, String jql) {
        if (!lastRemoteUpdatedDate.isPresent()) return jql;
            
        ZonedDateTime timeInJiraTz = ZonedDateTime.ofInstant(lastRemoteUpdatedDate.get().toInstant(), metadataService.getJiraTimeZone());
        String withTimeConstraint = "("+jql+") AND updated >= '" +ofPattern("yyyy-MM-dd HH:mm").format(timeInJiraTz)+"'";
        
        return withTimeConstraint;
    }
}
