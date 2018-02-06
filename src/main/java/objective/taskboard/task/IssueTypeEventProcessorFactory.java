package objective.taskboard.task;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import objective.taskboard.domain.Filter;
import objective.taskboard.issueBuffer.IssueBufferService;
import objective.taskboard.jira.JiraService;
import objective.taskboard.jira.WebhookSubtaskCreatorService;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.data.WebHookBody;
import objective.taskboard.jira.data.WebhookEvent;
import objective.taskboard.repository.FilterCachedRepository;
import retrofit.RetrofitError;

@Component
public class IssueTypeEventProcessorFactory implements JiraEventProcessorFactory {

    @Autowired
    private FilterCachedRepository filterCachedRepository;

    @Autowired
    private JiraService jiraService;

    @Autowired
    private IssueBufferService issueBufferService;

    @Autowired
    private WebhookSubtaskCreatorService webhookSubtaskCreatorService;

    @Override
    public Optional<JiraEventProcessor> create(WebHookBody body, String projectKey) {
        if(body.webhookEvent.category != WebhookEvent.Category.ISSUE)
            return Optional.empty();

        if(!belongsToAnyIssueTypeFilter(getIssueTypeIdOrNull(body)))
            return Optional.empty();

        return Optional.of(new IssueTypeEventProcessor(body));
    }

    protected boolean belongsToAnyIssueTypeFilter(Long issueTypeId) {
        if(issueTypeId == null)
            return true;

        List<Filter> filters = filterCachedRepository.getCache();
        return filters.stream().anyMatch(f -> issueTypeId.equals(f.getIssueTypeId()));
    }

    private Long getIssueTypeIdOrNull(WebHookBody body) {
        if(body.issue == null || body.issue.getIssueType() == null)
            return null;
        return body.issue.getIssueType().getId();
    }

    private class IssueTypeEventProcessor implements JiraEventProcessor {

        private final WebhookEvent webHook;
        private final String issueKey;
        public final WebHookBody.Changelog changelog;

        private IssueTypeEventProcessor(WebHookBody webHook) {
            this.webHook = webHook.webhookEvent;
            issueKey = webHook.issue.getKey();
            changelog = webHook.changelog;
        }

        @Override
        public String getDescription() {
            return "webhook: " + webHook.typeName + " issue: " + issueKey;
        }

        @Override
        public void processEvent() {
            Optional<JiraIssueDto> issue = fetchIssue();
            issue.ifPresent(jiraIssue ->
                    webhookSubtaskCreatorService.createSubtaskOnTransition(jiraIssue, changelog)
            );
            issueBufferService.updateByEvent(webHook, issueKey, issue);
        }

        private Optional<JiraIssueDto> fetchIssue() {
            if (webHook == WebhookEvent.ISSUE_DELETED)
                return Optional.empty();
            return Optional.ofNullable(jiraService.getIssueByKeyAsMaster(issueKey));
        }
    }
}
