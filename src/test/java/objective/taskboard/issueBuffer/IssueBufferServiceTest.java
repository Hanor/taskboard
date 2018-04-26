package objective.taskboard.issueBuffer;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.fasterxml.jackson.databind.ObjectMapper;

import objective.taskboard.data.Issue;
import objective.taskboard.data.Team;
import objective.taskboard.data.User;
import objective.taskboard.database.IssuePriorityService;
import objective.taskboard.domain.IssueColorService;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.domain.converter.IssueTeamService;
import objective.taskboard.domain.converter.JiraIssueToIssueConverter;
import objective.taskboard.domain.converter.JiraIssueToIssueConverterMockFactory;
import objective.taskboard.jira.JiraIssueService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.CustomField;
import objective.taskboard.jira.JiraProperties.CustomField.ClassOfServiceDetails;
import objective.taskboard.jira.JiraProperties.CustomField.TShirtSize;
import objective.taskboard.jira.JiraService;
import objective.taskboard.jira.MetadataService;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.data.Status;
import objective.taskboard.jira.data.WebHookBody;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.repository.TeamCachedRepository;
import objective.taskboard.testUtils.OptionalAutowiredDependenciesInitializer;

@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = OptionalAutowiredDependenciesInitializer.class, classes = IssueBufferServiceTest.Configuration.class)
public class IssueBufferServiceTest {

    public static class Configuration {
        @Bean
        private FactoryBean<JiraIssueToIssueConverter> jiraIssueToIssueConverter() {
            return new JiraIssueToIssueConverterMockFactory(
                    issueTeamService, 
                    metaDataService(), 
                    colorService(), 
                    getJiraProperties(), 
                    getIssuePriorityService());
        }

        @MockBean
        public JiraIssueService jiraIssueService;
        
        @Bean
        public CardRepoService cardRepoService() {
            return new CardRepoServiceMock();
        }

        @Bean
        public IssueBufferService issueBufferService() {
            return new IssueBufferService();
        }
        
        @MockBean
        public IssueTeamService issueTeamService;
        
        @Bean
        public MetadataService metaDataService() {
            MetadataService metadataService = mock(MetadataService.class);
            when(metadataService.getStatusById(10000L)).thenReturn(new Status(10000L, "Done", null));
            try {
                when(metadataService.getIssueTypeById(10001L)).thenReturn(new IssueType(null, 10001L, "Feature", false, "", new URI("http://foo")));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return metadataService;
        }
        
        public IssueColorService colorService() {
            return mock(IssueColorService.class);
        }
        
        private JiraProperties getJiraProperties() {
            JiraProperties jiraProperties = mock(JiraProperties.class);
            CustomField cf = mock(CustomField.class);
            when(cf.getClassOfService()).thenReturn(new ClassOfServiceDetails());
            TShirtSize ts = mock(TShirtSize.class);
            when(ts.getIds()).thenReturn(Arrays.asList());
            when(cf.getTShirtSize()).thenReturn(ts);
            when(jiraProperties.getCustomfield()).thenReturn(cf );
            return jiraProperties;
        }
        
        private IssuePriorityService getIssuePriorityService() {
            return mock(IssuePriorityService.class,Mockito.RETURNS_DEEP_STUBS);
        }
        
        @Bean
        public TeamCachedRepository teamRepo() {
            TeamCachedRepository teamrepo = Mockito.mock(TeamCachedRepository.class);
            Team team = new Team();
            team.setId(13L);
            when(teamrepo.findById(13L)).thenReturn(Optional.of(team));
            return teamrepo;
        }
        
        @MockBean
        private JiraService jiraBean;
        
        @Bean
        public ProjectFilterConfigurationCachedRepository getProjectRepo() {
            ProjectFilterConfigurationCachedRepository mock = mock(ProjectFilterConfigurationCachedRepository.class);
            ProjectFilterConfiguration proj = Mockito.mock(ProjectFilterConfiguration.class);
            when(proj.getDefaultTeam()).thenReturn(1L);
            when(mock.getProjectByKey("TASKB")).thenReturn(Optional.of(proj));
            return mock;
        }
    }

    private static final ObjectMapper objectMapper = new ObjectMapper()
                    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private JiraService jiraBean;

    @Autowired
    private IssueTeamService issueTeamService;

    @Autowired
    private IssueBufferService issueBufferService;

    @After
    public void removeAllIssues() {
        issueBufferService.reset();
    }

    @Test
    public void createThenConvert() throws IOException {
        WebHookBody payload1 = payload("create-TASKB-1.json");
        WebHookBody payload2 = payload("create-TASKB-2-subtaskof-TASKB-1.json");
        WebHookBody payload3 = payload("update-TASKB-2-converttotask.json");
        WebHookBody payload4 = payload("update-TASKB-2-converttosubtaskof-TASKB-1.json");

        // create parent
        issueBufferService.updateByEvent(payload1.webhookEvent, "TASKB-1", asJiraIssue(payload1.issue));
        assertThat(issueBufferService.getAllIssues()).hasSize(1);

        // create child
        issueBufferService.updateByEvent(payload2.webhookEvent, "TASKB-2", asJiraIssue(payload2.issue));
        assertThat(issueBufferService.getAllIssues()).hasSize(2);
        assertThat(issueBufferService.getIssueByKey("TASKB-1").getSubtasks()).hasSize(1);
        assertThat(issueBufferService.getIssueByKey("TASKB-2").getParent()).isEqualTo("TASKB-1");

        // convert child to task
        issueBufferService.updateByEvent(payload3.webhookEvent, "TASKB-2", asJiraIssue(payload3.issue));
        assertThat(issueBufferService.getAllIssues()).hasSize(2);
        assertThat(issueBufferService.getIssueByKey("TASKB-1").getSubtasks()).isEmpty();
        assertThat(issueBufferService.getIssueByKey("TASKB-2").getParent()).isNullOrEmpty();

        // undo convert
        issueBufferService.updateByEvent(payload4.webhookEvent, "TASKB-2", asJiraIssue(payload4.issue));
        assertThat(issueBufferService.getAllIssues()).hasSize(2);
        assertThat(issueBufferService.getIssueByKey("TASKB-1").getSubtasks()).hasSize(1);
        assertThat(issueBufferService.getIssueByKey("TASKB-2").getParent()).isEqualTo("TASKB-1");
    }

    @Test
    public void deleteIssue() throws IOException {
        WebHookBody payload1 = payload("create-TASKB-1.json");
        WebHookBody payload2 = payload("create-TASKB-2-subtaskof-TASKB-1.json");
        WebHookBody payload3 = payload("delete-TASKB-2.json");

        // create parent
        issueBufferService.updateByEvent(payload1.webhookEvent, "TASKB-1", asJiraIssue(payload1.issue));
        assertThat(issueBufferService.getAllIssues()).hasSize(1);

        // create child
        issueBufferService.updateByEvent(payload2.webhookEvent, "TASKB-2", asJiraIssue(payload2.issue));
        assertThat(issueBufferService.getAllIssues()).hasSize(2);
        assertThat(issueBufferService.getIssueByKey("TASKB-1").getSubtasks()).hasSize(1);
        assertThat(issueBufferService.getIssueByKey("TASKB-2").getParent()).isEqualTo("TASKB-1");

        // delete child
        issueBufferService.updateByEvent(payload3.webhookEvent, "TASKB-2", Optional.empty());
        assertThat(issueBufferService.getAllIssues()).hasSize(1);
        assertThat(issueBufferService.getIssueByKey("TASKB-1").getSubtasks()).isEmpty();
        assertThat(issueBufferService.getIssueByKey("TASKB-2")).isNull();
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void addTeamToIssue_ShouldAddTeamToIssueAndSendToJira() throws IOException {
        WebHookBody payload1 = payload("create-TASKB-1.json");
        issueBufferService.updateByEvent(payload1.webhookEvent, "TASKB-1", asJiraIssue(payload1.issue));
        when(jiraBean.getIssueByKey("TASKB-1")).thenReturn(asJiraIssue(payload1.issue));
        when(issueTeamService.getDefaultTeamId(Mockito.any())).thenReturn(1L);
        
        issueBufferService.addTeamToIssue("TASKB-1", 13L);
        
        ArgumentCaptor<String> issueKeyCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> teamIdsCaptor = ArgumentCaptor.forClass(List.class);

        verify(jiraBean).setTeams(issueKeyCaptor.capture(), teamIdsCaptor.capture());

        assertEquals("TASKB-1", issueKeyCaptor.getValue());
        assertEquals("1,13", teamIdsCaptor.getValue().stream().sorted().map(s->""+s).collect(joining(",")));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void addAssigneeToIssueWithoutAssignee_ShouldSendAListWithOneAssignee() {
        WebHookBody payload1 = payload("create-TASKB-1.json");
        issueBufferService.updateByEvent(payload1.webhookEvent, "TASKB-1", asJiraIssue(payload1.issue));

        issueBufferService.addAssigneeToIssue("TASKB-1", "johan");
        ArgumentCaptor<String> issueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> usernameCaptor = ArgumentCaptor.forClass(List.class);

        verify(jiraBean).assignToUsers(issueCaptor.capture(), usernameCaptor.capture());
        String actualIssue = issueCaptor.getValue();
        List<User> assigneeList = usernameCaptor.getValue();
        assertEquals("TASKB-1", actualIssue);
        assertEquals("johan", assigneeList.stream().map(a->a.name).collect(joining(",")));
    }

    @Test
    public void addAssigneeToIssueWithThatHasAssignee_ShouldNotInvokeService() {
        WebHookBody payload1 = payload("create-TASKB-1.json");
        issueBufferService.updateByEvent(payload1.webhookEvent, "TASKB-1", asJiraIssue(payload1.issue));
        Issue issue = issueBufferService.getIssueByKey("TASKB-1");
        issue.setAssignee(new User("johan"));

        issueBufferService.addAssigneeToIssue("TASKB-1", "johan");
        verifyNoMoreInteractions(jiraBean);
    }
  
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void removeAssigneeFromIssueWithItAsMainAssignee_AssigneeShouldBeUnassigned() {
        WebHookBody payload1 = payload("create-TASKB-1.json");
        issueBufferService.updateByEvent(payload1.webhookEvent, "TASKB-1", asJiraIssue(payload1.issue));
        Issue issue = issueBufferService.getIssueByKey("TASKB-1");
        issue.setAssignee(new User("johan"));
        issue.getCoAssignees().add(new User("albert"));

        issueBufferService.removeAssigneeFromIssue("TASKB-1", "johan");
        ArgumentCaptor<String> issueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> usernameCaptor = ArgumentCaptor.forClass(List.class);

        verify(jiraBean).assignToUsers(issueCaptor.capture(), usernameCaptor.capture());
        String actualIssue = issueCaptor.getValue();
        List<User> assigneeList = usernameCaptor.getValue();
        assertEquals("TASKB-1", actualIssue);
        assertEquals("albert", assigneeList.stream().map(a->a.name).collect(joining(",")));
    }

    public static Optional<JiraIssueDto> asJiraIssue(Map<String, Object> jsonObject) {
        try {
            return Optional.of(objectMapper.readValue(objectMapper.writeValueAsString(jsonObject), JiraIssueDto.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static WebHookBody payload(String file)  {
        try {
            return objectMapper.readValue(resolve("/webhook/" + file), WebHookBody.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static URL resolve(String resourceName) {
        return IssueBufferServiceTest.class.getResource(resourceName);
    }
}
