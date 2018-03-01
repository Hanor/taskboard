package objective.taskboard.jira;

import static java.util.Arrays.asList;
import static objective.taskboard.testUtils.DateTimeUtilSupport.date;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.runners.MockitoJUnitRunner;

import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.issueBuffer.CardRepo;
import objective.taskboard.repository.FilterCachedRepository;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;

@RunWith(MockitoJUnitRunner.class)
public class JiraIssueJqlBuilderServiceTest {
    @InjectMocks
    private JiraIssueJqlBuilderService subject;

    @Mock
    private ProjectFilterConfigurationCachedRepository projectRepository;

    @Mock
    private FilterCachedRepository filterRepository;
    
    @Mock
    private JiraProperties jiraProperties;
    
    @Mock
    private MetadataCachedService metadataService;
    
    @Mock
    private CardRepo cardsRepo;
    
    @Before
    public void setup() {
        when(metadataService.getJiraTimeZone()).thenReturn(ZoneId.of("America/Sao_Paulo"));
        when(cardsRepo.getCurrentProjects()).thenReturn(Optional.of(Sets.newSet("PROJ")));
    }
    
    @Test
    public void whenProjectsJql_produceProjectsJql() {
        ProjectFilterConfiguration projectFilterConfiguration = new ProjectFilterConfiguration();
        projectFilterConfiguration.setProjectKey("PROJ");
        when(projectRepository.getProjects()).thenReturn(asList(projectFilterConfiguration));
        
        String actual = subject.projectsSqlWithoutTimeConstraint().trim();
        
        assertEquals("project in ('PROJ')", actual);
    }
    
    @Test
    public void whenProjectsJqlWithDate_produceProjectsJqlWithUpdatedDate() {
        ProjectFilterConfiguration projectFilterConfiguration = new ProjectFilterConfiguration();
        projectFilterConfiguration.setProjectKey("PROJ");
        when(projectRepository.getProjects()).thenReturn(asList(projectFilterConfiguration));
        
        when(cardsRepo.getLastUpdatedDate()).thenReturn(Optional.of(date(2017,5,1,10,30)));
        String actual = subject.projectsJql(cardsRepo).trim();
        
        assertEquals("(project in ('PROJ') ) AND updated >= '2017-05-01 10:30'", actual);
    }
}