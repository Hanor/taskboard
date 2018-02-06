package objective.taskboard.issueBuffer;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import objective.taskboard.domain.converter.JiraIssueToIssueConverter;
import objective.taskboard.domain.converter.JiraIssueToIssueConverterMockFactory;
import objective.taskboard.jira.JiraIssueService;
import objective.taskboard.jira.data.WebHookBody;
import objective.taskboard.testUtils.OptionalAutowiredDependenciesInitializer;

@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = OptionalAutowiredDependenciesInitializer.class, classes = IssueBufferServiceTest.Configuration.class)
public class IssueBufferServiceTest {

    public static class Configuration {
        @Bean
        private FactoryBean<JiraIssueToIssueConverter> jiraIssueToIssueConverter() {
            return new JiraIssueToIssueConverterMockFactory();
        }

        @MockBean
        private JiraIssueService jiraIssueService;

        @Bean
        public CardRepoService cardRepoService() {
            return new CardRepoServiceMock();
        }

        @Bean
        public IssueBufferService issueBufferService() {
            return new IssueBufferService();
        }
    }

    private static final ObjectMapper objectMapper = new ObjectMapper()
                    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private IssueBufferService issueBufferService;

    @After
    public void resetIssueBuffer() {
        issueBufferService.reset();
    }

    @Test
    public void createThenConvert() throws IOException {
        WebHookBody payload1 = payload("create-TASKB-1.json");
        WebHookBody payload2 = payload("create-TASKB-2-subtaskof-TASKB-1.json");
        WebHookBody payload3 = payload("update-TASKB-2-converttotask.json");
        WebHookBody payload4 = payload("update-TASKB-2-converttosubtaskof-TASKB-1.json");

        // create parent
        issueBufferService.updateByEvent(payload1.webhookEvent, "TASKB-1", Optional.of(payload1.issue));
        assertThat(issueBufferService.getAllIssues()).hasSize(1);

        // create child
        issueBufferService.updateByEvent(payload2.webhookEvent, "TASKB-2", Optional.of(payload2.issue));
        assertThat(issueBufferService.getAllIssues()).hasSize(2);
        assertThat(issueBufferService.getIssueByKey("TASKB-1").getSubtasks()).hasSize(1);
        assertThat(issueBufferService.getIssueByKey("TASKB-2").getParent()).isEqualTo("TASKB-1");

        // convert child to task
        issueBufferService.updateByEvent(payload3.webhookEvent, "TASKB-2", Optional.of(payload3.issue));
        assertThat(issueBufferService.getAllIssues()).hasSize(2);
        assertThat(issueBufferService.getIssueByKey("TASKB-1").getSubtasks()).isEmpty();
        assertThat(issueBufferService.getIssueByKey("TASKB-2").getParent()).isNullOrEmpty();

        // undo convert
        issueBufferService.updateByEvent(payload4.webhookEvent, "TASKB-2", Optional.of(payload4.issue));
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
        issueBufferService.updateByEvent(payload1.webhookEvent, "TASKB-1", Optional.of(payload1.issue));
        assertThat(issueBufferService.getAllIssues()).hasSize(1);

        // create child
        issueBufferService.updateByEvent(payload2.webhookEvent, "TASKB-2", Optional.of(payload2.issue));
        assertThat(issueBufferService.getAllIssues()).hasSize(2);
        assertThat(issueBufferService.getIssueByKey("TASKB-1").getSubtasks()).hasSize(1);
        assertThat(issueBufferService.getIssueByKey("TASKB-2").getParent()).isEqualTo("TASKB-1");

        // delete child
        issueBufferService.updateByEvent(payload3.webhookEvent, "TASKB-2", Optional.empty());
        assertThat(issueBufferService.getAllIssues()).hasSize(1);
        assertThat(issueBufferService.getIssueByKey("TASKB-1").getSubtasks()).isEmpty();
        assertThat(issueBufferService.getIssueByKey("TASKB-2")).isNull();
    }

    public static WebHookBody payload(String file) throws IOException {
        return objectMapper.readValue(resolve("/webhook/" + file), WebHookBody.class);
    }

    private static URL resolve(String resourceName) {
        return IssueBufferServiceTest.class.getResource(resourceName);
    }
}
