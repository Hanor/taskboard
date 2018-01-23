package objective.taskboard.task;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import objective.taskboard.domain.Filter;
import objective.taskboard.issueBuffer.IssueBufferService;
import objective.taskboard.jira.JiraIssueService;
import objective.taskboard.jira.WebhookSubtaskCreatorService;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.data.WebHookBody;
import objective.taskboard.jira.data.WebhookEvent;
import objective.taskboard.repository.FilterCachedRepository;

@Component
public class IssueTypeEventProcessorFactory implements JiraEventProcessorFactory {

    @Autowired
    private FilterCachedRepository filterCachedRepository;

    @Autowired
    private JiraIssueService jiraIssueService;

    @Autowired
    private IssueBufferService issueBufferService;

    @Autowired
    private WebhookSubtaskCreatorService webhookSubtaskCreatorService;

    @Override
    public Optional<JiraEventProcessor> create(WebHookBody body, String projectKey) {
        if(!belongsToAnyIssueTypeFilter(getIssueTypeIdOrNull(body)))
            return Optional.empty();

        if(body.webhookEvent.category != WebhookEvent.Category.ISSUE)
            return Optional.empty();

        String issueKey = getIssueKey(body);
        return Optional.of(new IssueTypeEventProcessor(body.webhookEvent, issueKey, body.changelog));
    }

    protected boolean belongsToAnyIssueTypeFilter(Long issueTypeId) {
        if(issueTypeId == null)
            return true;

        List<Filter> filters = filterCachedRepository.getCache();
        return filters.stream().anyMatch(f -> issueTypeId.equals(f.getIssueTypeId()));
    }

    private String getIssueKey(WebHookBody body) {
        return (String) body.issue.get("key");
    }

    private Long getIssueTypeIdOrNull(WebHookBody body) {
        if(!body.issue.containsKey("issuetype"))
            return null;

        @SuppressWarnings("unchecked")
		Map<String, Object> issueType = (Map<String, Object>) body.issue.get("issuetype");
        return issueType == null ? null : Long.parseLong((String) issueType.get("id"));
    }

    private class IssueTypeEventProcessor implements JiraEventProcessor {

        private final WebhookEvent webHook;
        private final String issueKey;
        public final WebHookBody.Changelog changelog;

        private IssueTypeEventProcessor(WebhookEvent webHook, String issueKey, WebHookBody.Changelog changelog) {
            this.webHook = webHook;
            this.issueKey = issueKey;
            this.changelog = changelog;
        }

        @Override
        public String getDescription() {
            return "webhook: " + webHook.typeName + " issue: " + issueKey;
        }

        @Override
        public void processEvent() {
            Optional<JiraIssueDto> issue = fetchIssue();
            if (issue.isPresent()) {
                JiraIssueDto jiraIssue = issue.get();
                webhookSubtaskCreatorService.createSubtaskOnTransition(jiraIssue, changelog);
            }
            issueBufferService.updateByEvent(webHook, issueKey, issue);
        }

        private Optional<JiraIssueDto> fetchIssue() {
            if (webHook == WebhookEvent.ISSUE_DELETED)
                return Optional.empty();
            return jiraIssueService.searchIssueByKey(issueKey);
        }
    }
}
