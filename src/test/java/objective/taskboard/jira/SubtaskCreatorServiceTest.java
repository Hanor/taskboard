/*- 
 * [LICENSE] 
 * Taskboard 
 * - - - 
 * Copyright (C) 2015 - 2017 Objective Solutions 
 * - - - 
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU Affero General Public License 
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 * [/LICENSE] 
 */

package objective.taskboard.jira;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.api.domain.input.FieldInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;

import objective.taskboard.jira.JiraProperties.SubtaskCreation.CustomFieldCondition;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.client.JiraIssueFieldDto;
import objective.taskboard.jira.client.JiraIssueTypeDto;
import objective.taskboard.jira.client.JiraPriorityDto;
import objective.taskboard.jira.client.JiraProjectDto;
import objective.taskboard.jira.client.JiraSubtaskDto;
import objective.taskboard.jira.client.JiraUserDto;
import objective.taskboard.jira.data.Transition;

@RunWith(MockitoJUnitRunner.class)
public class SubtaskCreatorServiceTest {

    private final long PRIORITY_ID = 45689L;
    private final long ISSUE_TYPE = 9L;
    private final String TSHIRT_PARENT_ID = "customfield_11111";
    private final String TSHIRT_SUBTASK_ID = "customfield_22222";
    private final String CLASSOFSERVICE_ID = "customfield_33333";
    private final String CUSTOM_FIELD_CONDITION_ID = "customfield_44444";
    private final String CUSTOM_FIELD_CONDITION_VALUE = "Yes";

    @Mock
    private JiraIssueDto parent;
    @Mock
    private JiraProjectDto parentProject;
    @Mock
    private JiraPriorityDto parentPriority;
    @Mock
    private JiraUserDto parentReporter;
    @Mock
    private JiraService jiraService;
    @Mock
    private Issue subtask;
    
    @Captor
    ArgumentCaptor<IssueInput> issueInputCaptor;

    private JiraProperties jiraProperties = new JiraProperties();
    private JiraProperties.SubtaskCreation properties = new JiraProperties.SubtaskCreation();
    private SubtaskCreatorService service;
    
    @Before
    public void setup() throws Exception {
        service = new SubtaskCreatorService(jiraService, jiraProperties);
        jiraProperties.getCustomfield().getClassOfService().setId(CLASSOFSERVICE_ID);
        
        setupProperties();
        setupParentMock();
        
        when(jiraService.createIssueAsMaster(any(IssueInput.class))).thenReturn("TASK-101");
        when(subtask.getKey()).thenReturn("TASK-101");
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(jiraService);
    }
    
    @Test
    public void ifThereIsAlreadyASubtaskOfTheSameType_dontCreateSubtask() {
        List<JiraSubtaskDto> subtasks = asList(subtask(222, "TASK-201"), subtask(ISSUE_TYPE, "TASK-202"), subtask(333, "TASK-203"));
        when(parent.getSubtasks()).thenReturn(subtasks);
        
        service.create(parent, properties);
        
        verifyZeroInteractions(jiraService);
    }
    
    @Test
    public void whenParentHaveAllFields_useFieldsValueFromParent() {
        service.create(parent, properties);
        
        verify(jiraService).createIssueAsMaster(issueInputCaptor.capture());
        assertIssueInput(issueInputCaptor.getValue(),
                "project=TASK",
                "issuetype=9",
                "parent=TASK-100",
                "summary=SUB - My Parent Summary",
                "reporter=my.user",
                "priority=45689",
                "customfield_22222=L",
                "customfield_33333=685321"
        );
    }

    @Test
    public void givenRequiredCustomFieldFilled_whenParentHasNoRequiredValue_thenSubtaskCreationHasToBeSkipped() {
        properties.setCustomFieldCondition(makeCustomFieldCondition(CUSTOM_FIELD_CONDITION_ID, CUSTOM_FIELD_CONDITION_VALUE));

        JSONArray issueFieldValue = null;
        when(parent.getField(CUSTOM_FIELD_CONDITION_ID)).thenReturn(new JiraIssueFieldDto(issueFieldValue));

        service.create(parent, properties);
        verifyZeroInteractions(jiraService);
    }

    @Test
    public void givenRequiredCustomFieldFilled_whenParentHasRequiredValue_thenCreateSubtask() throws JSONException {
        properties.setCustomFieldCondition(makeCustomFieldCondition(CUSTOM_FIELD_CONDITION_ID, CUSTOM_FIELD_CONDITION_VALUE));

        JSONArray issueFieldValue = new JSONArray("[{value:Yes}]");
        when(parent.getField(CUSTOM_FIELD_CONDITION_ID)).thenReturn(new JiraIssueFieldDto(issueFieldValue));

        service.create(parent, properties);
        verify(jiraService, only()).createIssueAsMaster(any());
    }

    @Test
    public void whenParentHaveNoTShirtSize_useTShirtSizeDefaultValue() {
        when(parent.getField(TSHIRT_PARENT_ID)).thenReturn(null);
        
        service.create(parent, properties);
        
        verify(jiraService).createIssueAsMaster(issueInputCaptor.capture());
        assertIssueInput(issueInputCaptor.getValue(),
                "project=TASK",
                "issuetype=9",
                "parent=TASK-100",
                "summary=SUB - My Parent Summary",
                "reporter=my.user",
                "priority=45689",
                "customfield_22222=M",
                "customfield_33333=685321"
        );
    }
    
    @Test
    public void whenParentHaveNoClassOfService_keepItNull() {
        when(parent.getField(CLASSOFSERVICE_ID)).thenReturn(null);
        
        service.create(parent, properties);
        
        verify(jiraService).createIssueAsMaster(issueInputCaptor.capture());
        assertIssueInput(issueInputCaptor.getValue(),
                "project=TASK",
                "issuetype=9",
                "parent=TASK-100",
                "summary=SUB - My Parent Summary",
                "reporter=my.user",
                "priority=45689",
                "customfield_22222=L",
                "customfield_33333=null"
        );
    }
    
    @Test
    public void whenTransitionIsConfigured_executeTransition() {
        properties.setTransitionId(Optional.of(57L));
        configureSubtaskTransitions(1L, 57L, 3L);
        
        service.create(parent, properties);
        
        verify(jiraService).createIssueAsMaster(issueInputCaptor.capture());
        assertIssueInput(issueInputCaptor.getValue(),
                "project=TASK",
                "issuetype=9",
                "parent=TASK-100",
                "summary=SUB - My Parent Summary",
                "reporter=my.user",
                "priority=45689",
                "customfield_22222=L",
                "customfield_33333=685321"
        );
        
        verify(jiraService).getTransitionsAsMaster(subtask.getKey());
        verify(jiraService).doTransitionAsMaster(subtask.getKey(), 57L);
    }
    
    @Test
    public void whenSubtaskAlreadyExistsAndTransitionIsConfigured_onlyExecuteTransition() {
        properties.setTransitionId(Optional.of(57L));
        configureSubtaskTransitions(1L, 57L, 3L);
        
        List<JiraSubtaskDto> subtasks = asList(subtask(ISSUE_TYPE, "TASK-101"));
        when(parent.getSubtasks()).thenReturn(subtasks);
        
        service.create(parent, properties);
        
        verify(jiraService, never()).createIssueAsMaster(any());
        verify(jiraService).getTransitionsAsMaster(subtask.getKey());
        verify(jiraService).doTransitionAsMaster(subtask.getKey(), 57L);
    }
    
    @Test
    public void whenTransitionIsNotAvailable_dontTryToExecuteTransition() {
        properties.setTransitionId(Optional.of(57L));
        configureSubtaskTransitions(1L, 2L, 3L);
        
        service.create(parent, properties);
        
        verify(jiraService).createIssueAsMaster(any(IssueInput.class));
        verify(jiraService).getTransitionsAsMaster(subtask.getKey());
    }
    
    @Test
    public void givenSkipCreationConfiguredWithTrue_whenParentTShirtSizeIsNull_thenDontCreateSubTask() {
        properties.setSkipCreationWhenTShirtParentIsAbsent(true);
        when(parent.getField(TSHIRT_PARENT_ID)).thenReturn(null);
        
        service.create(parent, properties);
        verifyZeroInteractions(jiraService);
    }
    
    @Test
    public void givenSkipCreationConfiguredWithTrue_whenParentTShirtSizeIsPresent_thenCreateSubTask() {
        properties.setSkipCreationWhenTShirtParentIsAbsent(true);
        
        service.create(parent, properties);
        verify(jiraService).createIssueAsMaster(any(IssueInput.class));
    }
    
    private void assertIssueInput(IssueInput input, String... expecteds) {
        List<String> representations = new ArrayList<>();
        representations.add(getFieldRepresentation(input, "project", "key"));
        representations.add(getFieldRepresentation(input, "issuetype", "id"));
        representations.add(getFieldRepresentation(input, "parent", "key"));
        representations.add(getFieldRepresentation(input, "summary", null));
        representations.add(getFieldRepresentation(input, "reporter", "name"));
        representations.add(getFieldRepresentation(input, "priority", "id"));
        representations.add(getFieldRepresentation(input, "customfield_22222", "value"));
        representations.add(getFieldRepresentation(input, "customfield_33333", "id"));
        
        String current = String.join("\n", representations);
        String expected = String.join("\n", expecteds);
        
        Assert.assertEquals(expected, current);
    }
    
    private String getFieldRepresentation(IssueInput input, String field, String valueKey) {
        String representation = field + "=";
        FieldInput fieldInput = input.getField(field);
        if (fieldInput == null || fieldInput.getValue() == null)
            return representation + "null";
        
        Object value = fieldInput.getValue();
        if (value instanceof ComplexIssueInputFieldValue) {
            ComplexIssueInputFieldValue complex = (ComplexIssueInputFieldValue) value;
            if (complex != null)
                return representation + complex.getValuesMap().get(valueKey);
        }
        return representation + value;        
    }
    
    private void setupProperties() {
        properties.setIssueTypeId(ISSUE_TYPE);
        properties.setSummaryPrefix("SUB - ");
        properties.setTShirtSizeParentId(TSHIRT_PARENT_ID);
        properties.setTShirtSizeSubtaskId(TSHIRT_SUBTASK_ID);
        properties.setTShirtSizeDefaultValue("M");
        properties.setTransitionId(Optional.ofNullable(null));
    }

    private void setupParentMock() throws JSONException {
        when(parent.getKey()).thenReturn("TASK-100");
        when(parent.getSummary()).thenReturn("My Parent Summary");
        when(parent.getProject()).thenReturn(parentProject);
        when(parent.getPriority()).thenReturn(parentPriority);
        when(parent.getReporter()).thenReturn(parentReporter);
        when(parent.getField(TSHIRT_PARENT_ID)).thenReturn(new JiraIssueFieldDto(new JSONObject("{value=L}")));
        when(parent.getField(CLASSOFSERVICE_ID)).thenReturn(new JiraIssueFieldDto(new JSONObject("{id=685321}")));
        
        when(parentReporter.getSelf()).thenReturn("http://foo/my.user");
        when(parentReporter.getName()).thenReturn("my.user");
        when(parentProject.getKey()).thenReturn("TASK");
        when(parentPriority.getId()).thenReturn(PRIORITY_ID);
        
        List<JiraSubtaskDto> subtasks = asList(subtask(222, "TASK-201"), subtask(333, "TASK-202"));
        when(parent.getSubtasks()).thenReturn(subtasks);
    }

    private JiraSubtaskDto subtask(long typeId, String key) {
        JiraIssueTypeDto type = mock(JiraIssueTypeDto.class);
        when(type.getId()).thenReturn((long)typeId);

        JiraSubtaskDto subtask = mock(JiraSubtaskDto.class);
        when(subtask.getIssueType()).thenReturn(type);
        when(subtask.getIssueKey()).thenReturn(key);
        return subtask;
    }
    
    private void configureSubtaskTransitions(Long... transitionsId) {
        List<Transition> transitions = new ArrayList<>();
        for (Long id : transitionsId) {
            Transition transition = new Transition(id, null, null, null);
            transitions.add(transition);
        }
        when(jiraService.getTransitionsAsMaster(subtask.getKey())).thenReturn(transitions);
    }

    private CustomFieldCondition makeCustomFieldCondition(String id, String value) {
        CustomFieldCondition customFieldCondition = new CustomFieldCondition();
        customFieldCondition.setId(id);
        customFieldCondition.setValue(value);
        return customFieldCondition;
    }

}
