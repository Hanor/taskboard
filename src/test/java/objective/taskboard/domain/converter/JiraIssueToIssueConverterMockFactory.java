package objective.taskboard.domain.converter;

import static objective.taskboard.domain.converter.IssueFieldsExtractor.convertWorklog;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractChangelog;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractComponents;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractLabels;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractRealParent;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.stream.Collectors;

import org.springframework.beans.factory.FactoryBean;

import objective.taskboard.data.IssueScratch;
import objective.taskboard.data.TaskboardTimeTracking;
import objective.taskboard.data.User;
import objective.taskboard.database.IssuePriorityService;
import objective.taskboard.domain.IssueColorService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.MetadataService;
import objective.taskboard.jira.client.JiraIssueDto;

public class JiraIssueToIssueConverterMockFactory implements FactoryBean<JiraIssueToIssueConverter> {

    private IssueTeamService issueTeamService;
    private MetadataService metadataService;
    private IssueColorService issueColorService;
    private JiraProperties jiraProperties;
    private IssuePriorityService issuePriorityService;

    public JiraIssueToIssueConverterMockFactory(IssueTeamService issueTeamService, MetadataService metadataService, IssueColorService issueColorService, JiraProperties jiraProperties, IssuePriorityService issuePriorityService) {
        this.issueTeamService = issueTeamService;
        this.metadataService = metadataService;
        this.issueColorService = issueColorService;
        this.jiraProperties = jiraProperties;
        this.issuePriorityService = issuePriorityService;
    }

    @Override
    public JiraIssueToIssueConverter getObject() throws Exception {
        JiraIssueToIssueConverter jiraIssueToIssueConverter = mock(JiraIssueToIssueConverter.class);
        willAnswer(invocation -> {
            JiraIssueDto jiraIssue = invocation.getArgumentAt(0, JiraIssueDto.class);
            ParentProvider provider = invocation.getArgumentAt(1, ParentProvider.class);

            String parentKey = extractRealParent(jiraIssue);

            LinkedList<Long> assignedTeamsIds = new LinkedList<>();
            if (jiraIssue.getField("customfield_TEAMS")!=null)
                assignedTeamsIds.addAll(Arrays.asList(jiraIssue.getField("customfield_TEAMS").toString().split(",")).stream().map(m->Long.parseLong(m)).collect(Collectors.toList()));
            
            IssueScratch scratch = new IssueScratch(
                    jiraIssue.getId(),
                    jiraIssue.getKey(),
                    jiraIssue.getProject().getKey(),
                    jiraIssue.getProject().getName(),
                    jiraIssue.getIssueType().getId(),
                    defaultIfNull(jiraIssue.getSummary(), ""),
                    jiraIssue.getStatus().getId(),
                    0L,
                    parentKey,
                    new LinkedList<>(),
                    new LinkedList<>(),
                    User.from(jiraIssue.getAssignee()),
                    jiraIssue.getPriority() != null ? jiraIssue.getPriority().getId() : 0L,
                    jiraIssue.getDueDate() != null ? jiraIssue.getDueDate().toDate() : null,
                    jiraIssue.getCreationDate().getMillis(),
                    jiraIssue.getUpdateDate() != null ? jiraIssue.getUpdateDate().toDate() : jiraIssue.getCreationDate().toDate(),
                    defaultIfNull(jiraIssue.getDescription(), ""),
                    "",
                    extractLabels(jiraIssue),
                    extractComponents(jiraIssue),
                    false,
                    null,
                    new LinkedHashMap<>(),
                    null,
                    TaskboardTimeTracking.fromJira(jiraIssue.getTimeTracking()),
                    jiraIssue.getReporter() != null ? jiraIssue.getReporter().getName() : null,
                    null,
                    null,
                    extractChangelog(jiraIssue),
                    convertWorklog(jiraIssue.getWorklogs()),
                    assignedTeamsIds);
            return jiraIssueToIssueConverter.createIssueFromScratch(scratch, provider);
        }).given(jiraIssueToIssueConverter).convertSingleIssue(any(), any());
        
        when(jiraIssueToIssueConverter.getIssueTeamService()).thenReturn(issueTeamService);
        when(jiraIssueToIssueConverter.getMetadataService()).thenReturn(metadataService);
        when(jiraIssueToIssueConverter.getIssueColorService()).thenReturn(issueColorService);
        when(jiraIssueToIssueConverter.getJiraProperties()).thenReturn(jiraProperties);
        when(jiraIssueToIssueConverter.getIssuePriorityService()).thenReturn(issuePriorityService);
        willCallRealMethod().given(jiraIssueToIssueConverter).createIssueFromScratch(any(), any());

        return jiraIssueToIssueConverter;
    }

    @Override
    public Class<?> getObjectType() {
        return JiraIssueToIssueConverter.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
