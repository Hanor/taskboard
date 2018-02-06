package objective.taskboard.issueBuffer;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.util.Arrays.asList;
import static objective.taskboard.issueBuffer.IssueBufferServiceTest.payload;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

import java.io.IOException;

import objective.taskboard.jira.data.WebhookEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import objective.taskboard.controller.WebhookController;
import objective.taskboard.domain.Filter;
import objective.taskboard.domain.converter.JiraIssueToIssueConverter;
import objective.taskboard.domain.converter.JiraIssueToIssueConverterMockFactory;
import objective.taskboard.jira.JiraService;
import objective.taskboard.jira.WebhookSubtaskCreatorService;
import objective.taskboard.jira.data.WebHookBody;
import objective.taskboard.repository.FilterCachedRepository;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.task.IssueEventProcessScheduler;
import objective.taskboard.task.IssueTypeEventProcessorFactory;
import objective.taskboard.testUtils.OptionalAutowiredDependenciesInitializer;

@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = OptionalAutowiredDependenciesInitializer.class, classes = WebhookControllerTest.Configuration.class)
public class WebhookControllerTest {

    public static class Configuration {
        @MockBean
        private ProjectFilterConfigurationCachedRepository projectFilterConfigurationCachedRepository;

        @MockBean
        private JiraService jiraService;

        @MockBean
        private WebhookSubtaskCreatorService webhookSubtaskCreatorService;

        @MockBean
        private FilterCachedRepository filterCachedRepository;

        @MockBean
        private IssueBufferService issueBufferService;

        @Bean
        public FactoryBean<JiraIssueToIssueConverter> jiraIssueToIssueConverter() {
            return new JiraIssueToIssueConverterMockFactory();
        }

        @Bean
        public ObjectMapper mapper() {
            return new ObjectMapper()
                    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        @Bean
        public WebhookController webhookController() {
            return new WebhookController();
        }

        @Bean
        public IssueTypeEventProcessorFactory issueTypeEventProcessorFactory() {
            return new IssueTypeEventProcessorFactory();
        }

        @Bean
        public IssueEventProcessScheduler issueEventProcessScheduler() {
            return new IssueEventProcessScheduler();
        }
    }

    @Autowired
    private Configuration mocks;

    @Autowired
    private WebhookController webhookController;

    @Autowired
    private IssueEventProcessScheduler issueEventProcessScheduler;

    @Before
    public void setup() {
        willReturn(true).given(mocks.projectFilterConfigurationCachedRepository).exists(eq("TASKB"));

        Filter taskFilter = new Filter();
        taskFilter.setIssueTypeId(10000L);
        Filter subtaskFilter = new Filter();
        subtaskFilter.setIssueTypeId(10001L);
        willReturn(asList(taskFilter, subtaskFilter)).given(mocks.filterCachedRepository).getCache();
    }

    @Test
    public void create() throws IOException {
        WebHookBody payload1 = payload("create-TASKB-1.json");
        WebHookBody payload2 = payload("create-TASKB-2-subtaskof-TASKB-1.json");

        given(mocks.jiraService.getIssueByKeyAsMaster(eq("TASKB-1")))
                .willReturn(payload1.issue);
        given(mocks.jiraService.getIssueByKeyAsMaster(eq("TASKB-2")))
                .willReturn(payload2.issue);

        // create parent
        webhookController.webhook(payload1, "TASKB");
        // create child
        webhookController.webhook(payload2, "TASKB");

        issueEventProcessScheduler.processItems();

        then(mocks.issueBufferService).should().updateByEvent(eq(WebhookEvent.ISSUE_CREATED), eq("TASKB-1"), any());
        then(mocks.issueBufferService).should().updateByEvent(eq(WebhookEvent.ISSUE_CREATED), eq("TASKB-2"), any());
    }

    @Test
    public void update() throws IOException {
        WebHookBody payload1 = payload("update-TASKB-2-converttotask.json");
        WebHookBody payload2 = payload("update-TASKB-2-converttosubtaskof-TASKB-1.json");

        given(mocks.jiraService.getIssueByKeyAsMaster(eq("TASKB-2")))
                .willReturn(payload1.issue, payload2.issue);

        // convert child to task
        webhookController.webhook(payload1, "TASKB");
        // undo convert
        webhookController.webhook(payload2, "TASKB");

        issueEventProcessScheduler.processItems();

        then(mocks.issueBufferService).should(times(2)).updateByEvent(eq(WebhookEvent.ISSUE_UPDATED), eq("TASKB-2"), any());
    }

    @Test
    public void delete() throws IOException {
        WebHookBody payload = payload("delete-TASKB-2.json");

        given(mocks.jiraService.getIssueByKeyAsMaster(eq("TASKB-2")))
                .willReturn(payload.issue);

        // delete child
        webhookController.webhook(payload, "TASKB");

        issueEventProcessScheduler.processItems();

        then(mocks.issueBufferService).should().updateByEvent(eq(WebhookEvent.ISSUE_DELETED), eq("TASKB-2"), any());
    }
}
