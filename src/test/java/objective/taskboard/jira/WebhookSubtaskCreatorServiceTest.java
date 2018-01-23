package objective.taskboard.jira;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import objective.taskboard.jira.JiraProperties.SubtaskCreation;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.client.JiraIssueTypeDto;
import objective.taskboard.jira.data.WebHookBody;

@RunWith(MockitoJUnitRunner.class)
public class WebhookSubtaskCreatorServiceTest {

    @Mock
    private JiraIssueDto parent;
    @Mock
    private JiraIssueTypeDto issueType;
    @Mock
    private SubtaskCreatorService subtaskCreatorService;
    
    private WebhookSubtaskCreatorService service;
    private JiraProperties jiraProperties = new JiraProperties();
    private SubtaskCreation properties1 = new SubtaskCreation();
    private SubtaskCreation properties2 = new SubtaskCreation();
    
    private WebHookBody.Changelog changelog = new WebHookBody.Changelog();
    private List<Map<String, Object>> changelogItems = new ArrayList<>();

    @Before
    public void setup() throws Exception {
        properties1.setIssueTypeParentId(88L);
        properties1.setStatusIdFrom(8888L);
        properties1.setStatusIdTo(9999L);
        
        properties2.setIssueTypeParentId(22L);
        properties2.setStatusIdFrom(2222L);
        properties2.setStatusIdTo(3333L);
        
        service = new WebhookSubtaskCreatorService(subtaskCreatorService, jiraProperties);
        jiraProperties.setSubtaskCreation(asList(properties1, properties2));
        
        when(parent.getIssueType()).thenReturn(issueType);
        changelog.items = changelogItems;
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(subtaskCreatorService);
    }
    
    @Test
    public void dontCreateSubtask_ifStatusHaventChanged() {
        service.createSubtaskOnTransition(parent, changelog);

        addChange("other", new Object());
        service.createSubtaskOnTransition(parent, changelog);
        
        verifyZeroInteractions(subtaskCreatorService);
    }

    @Test
    public void dontCreateSubtask_ifChangelogWasNotProvided() {
        service.createSubtaskOnTransition(parent, null);

        verifyZeroInteractions(subtaskCreatorService);
    }
    
    @Test
    public void createSubtask_whenConfigurationMatchesTransition() {
        configureTransition(22L, 2222L, 3333L);
        service.createSubtaskOnTransition(parent, changelog);
        verify(subtaskCreatorService).create(parent, properties2);
        
        configureTransition(88L, 8888L, 9999L);
        service.createSubtaskOnTransition(parent, changelog);
        verify(subtaskCreatorService).create(parent, properties1);
    }
    
    @Test
    public void dontCreateSubstask_whenIssueTypeParentIdDoesntMatch() {
        configureTransition(55L, 2222L, 3333L);
        verifyZeroInteractions(subtaskCreatorService);
    }
    
    @Test
    public void dontCreateSubstask_whenStatusFromIdDoesntMatch() {
        configureTransition(22L, 5555L, 3333L);
        verifyZeroInteractions(subtaskCreatorService);
    }

    @Test
    public void dontCreateSubstask_whenStatusToIdDoesntMatch() {
        configureTransition(22L, 2222L, 5555L);
        verifyZeroInteractions(subtaskCreatorService);
    }

    @Test
    public void dontCreateSubtask_whenIssueIsNull() {
        configureTransition(22L, 2222L, 3333L);
        service.createSubtaskOnTransition(null, changelog);
        verifyZeroInteractions(subtaskCreatorService);
    }

    private void configureTransition(Long issueTypeId, Long statusFromId, Long statusToId) {
        when(issueType.getId()).thenReturn(issueTypeId);
        changelogItems.clear();
        changelogItems.add(addStatusChange(statusFromId.toString(), statusToId.toString()));
    }    
    
    private void addChange(String field, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(field, value);
        changelogItems.add(map);
    }
    
    private Map<String, Object> addStatusChange(String from, String to) {
        Map<String, Object> map = new HashMap<>();        
        map.put("field", "status");
        map.put("from", from);
        map.put("to", to);
        return map;
    }

}
