/*-
 * [LICENSE]
 * Taskboard
 * - - -
 * Copyright (C) 2015 - 2016 Objective Solutions
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
package objective.taskboard.domain.converter;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.jira.rest.client.api.domain.IssueType;

import objective.taskboard.data.Issue;
import objective.taskboard.database.IssuePriorityService;
import objective.taskboard.domain.IssueColorService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.CustomField;
import objective.taskboard.jira.JiraProperties.CustomField.Blocked;
import objective.taskboard.jira.JiraProperties.CustomField.ClassOfServiceDetails;
import objective.taskboard.jira.JiraProperties.CustomField.CustomFieldDetails;
import objective.taskboard.jira.JiraProperties.CustomField.TShirtSize;
import objective.taskboard.jira.JiraService;
import objective.taskboard.jira.MetadataService;
import objective.taskboard.jira.client.JiraCommentDto;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.client.JiraIssueTypeDto;
import objective.taskboard.jira.client.JiraPriorityDto;
import objective.taskboard.jira.client.JiraProjectDto;
import objective.taskboard.jira.client.JiraStatusDto;
import objective.taskboard.jira.client.JiraUserDto;
import objective.taskboard.jira.client.JiraWorklogResultSetDto;
import objective.taskboard.repository.FilterCachedRepository;
import objective.taskboard.repository.ParentIssueLinkRepository;

@RunWith(MockitoJUnitRunner.class)
public class JiraIssueToIssueConverterTest {

    private static final String PARENT_ISSUE_KEY = "ISSUE-1";
    private static final String ISSUE_KEY = "ISSUE-2";
    private static final String TYPE_ICON_URI = "iconURI";
    private static final String CLASS_OF_SERVICE_EXPEDITE = "Expedite";
    private static final String CLASS_OF_SERVICE_STANDARD = "Standard";

    private static final String JSON_PARENT = "{key:'%s'}";
    private static final String JSON_CLASS_OF_SERVICE = "{id:1, value:'%s'}";

    private static final String PARENT_ID = "parent";
    private static final String CLASS_OF_SERVICE_ID = "classOfServiceId";
    private static final String CO_ASSIGNEES_ID = "coAssigneesId";
    private static final String BLOCKED_ID = "blockedId";
    private static final String LAST_BLOCK_REASON_ID = "lastBlockReasonId";
    private static final String ADDITIONAL_ESTIMATED_HOURS_ID = "additionalEstimatedHoursId";
    private static final String RELEASE_ID = "releaseId";

    @InjectMocks
    private JiraIssueToIssueConverter subject;

    @Mock
    private ParentIssueLinkRepository parentIssueLinkRepository;
    @Mock
    private JiraIssueDto issue;
    @Mock
    private JiraIssueDto parent;
    @Mock
    private JiraProjectDto project;
    @Mock
    private JiraPriorityDto priority;
    @Mock
    private JiraIssueTypeDto issueType;
    @Mock
    private JiraStatusDto status;
    @Mock
    private JiraUserDto assignee;
    @Mock
    private JiraProperties jiraProperties;
    @Mock
    private CustomField customField;
    @Mock
    private CustomFieldDetails coAssigneesDetails;
    @Mock
    private Blocked blocked;
    @Mock
    private TShirtSize tShirtSize;
    @Mock
    private CustomFieldDetails lastBlockReason;
    @Mock
    private CustomFieldDetails additionalEstimatedHours;
    @Mock
    private CustomFieldDetails release;
    @Mock
    private ClassOfServiceDetails classOfServiceDetails;
    @Mock
    private objective.taskboard.jira.JiraProperties.IssueLink issueLinkProperty;
    @Mock
    private IssueTeamService issueTeamService;
    @Mock
    private StartDateStepService startDateStepService;
    @Mock
    private IssueColorService issueColorService;
    @Mock
    private JiraService jiraService;
    @Mock
    private IssuePriorityService priorityService;
    @Mock
    private MetadataService metadataService;
    @Mock
    private JiraCommentDto comment;
    @Mock
    private FilterCachedRepository filterRepository;
    @Mock
    private CardVisibilityEvalService cardVisibilityEvalService;
    
    @Before
    public void before() {
        when(parentIssueLinkRepository.findAll()).thenReturn(asList());

        when(classOfServiceDetails.getId()).thenReturn(CLASS_OF_SERVICE_ID);
        when(customField.getClassOfService()).thenReturn(classOfServiceDetails);

        when(coAssigneesDetails.getId()).thenReturn(CO_ASSIGNEES_ID);
        when(customField.getCoAssignees()).thenReturn(coAssigneesDetails);

        when(blocked.getId()).thenReturn(BLOCKED_ID);
        when(customField.getBlocked()).thenReturn(blocked);

        when(tShirtSize.getIds()).thenReturn(asList());
        when(customField.getTShirtSize()).thenReturn(tShirtSize);

        when(lastBlockReason.getId()).thenReturn(LAST_BLOCK_REASON_ID);
        when(customField.getLastBlockReason()).thenReturn(lastBlockReason);

        when(additionalEstimatedHours.getId()).thenReturn(ADDITIONAL_ESTIMATED_HOURS_ID);
        when(customField.getAdditionalEstimatedHours()).thenReturn(additionalEstimatedHours);

        when(release.getId()).thenReturn(RELEASE_ID);
        when(customField.getRelease()).thenReturn(release);

        when(customField.getAssignedTeams()).thenReturn(mock(CustomFieldDetails.class));
        when(customField.getCoAssignees()).thenReturn(mock(CustomFieldDetails.class));
        
        when(jiraProperties.getCustomfield()).thenReturn(customField);

        mockIssue(issue, ISSUE_KEY);

        when(priorityService.determinePriority(any())).thenReturn(0L);

        when(cardVisibilityEvalService.calculateVisibleUntil(any(), any(), any())).thenReturn(Optional.empty());

        IssueType metadataIssueType = new IssueType(null, 1L, null, false, null, URI.create(TYPE_ICON_URI));
        when(metadataService.getIssueTypeById(any())).thenReturn(metadataIssueType);
    }

    @Test
    public void simpleIssueConvert() {
        when(project.getKey()).thenReturn("ISSUE");
        when(project.getName()).thenReturn("PROJECT");
        when(issueType.getId()).thenReturn(1L);
        when(issue.getSummary()).thenReturn("Summary");
        when(status.getId()).thenReturn(1L);
        when(startDateStepService.get(issue)).thenReturn(1L);
        when(assignee.getAvatarUri(anyString())).thenReturn(URI.create("assigneeAvatarURI"));
        when(assignee.getName()).thenReturn("assignee");
        when(issue.getAssignee()).thenReturn(assignee);
        when(priority.getId()).thenReturn(1L);
        when(issue.getDueDate()).thenReturn(new DateTime(0));
        when(issue.getDescription()).thenReturn("Description");

        Issue.CardTeam cardTeam = new Issue.CardTeam();
        cardTeam.name = "team";
        when(issueTeamService.getTeamsForIds(any())).thenReturn(Sets.newSet(cardTeam));

        objective.taskboard.data.Issue converted = subject.convertSingleIssue(issue, buildProvider());

        assertEquals("Issue key", ISSUE_KEY, converted.getIssueKey());
        assertEquals("Project key", "ISSUE", converted.getProjectKey());
        assertEquals("Project name", "PROJECT", converted.getProject());
        assertEquals("Issue type id", 1L, converted.getType());
        assertEquals("Issue type icon URI", TYPE_ICON_URI, converted.getTypeIconUri());
        assertEquals("Summary", "Summary", converted.getSummary());
        assertEquals("Status id", 1L, converted.getStatus());
        assertEquals("Start date step millis", 1L, converted.getStartDateStepMillis());
        assertEquals("Parent key", "", converted.getParent());
        assertEquals("Parent type id", 0L, converted.getParentType());
        assertEquals("Parent type icon URI", "", converted.getParentTypeIconUri());
        assertEquals("Dependencies quantity", 0, converted.getDependencies().size());
        assertEquals("Co-assignees", 0, converted.getCoAssignees().size());
        assertEquals("Assignee name", "assignee", converted.getAssignee().name);
        assertEquals("Priority", 1L, converted.getPriority());
        assertEquals("Due date", new Date(0), converted.getDueDate());
        assertEquals("Creation date millis", new DateTime(0).getMillis(), converted.getCreated());
        assertEquals("Description", "Description", converted.getDescription());
        assertEquals("Teams", 1, converted.getTeams().size());
        assertEquals("Team name", "team", converted.getTeams().iterator().next().name);
        assertEquals("Comments", "", converted.getComments());
        assertFalse("Blocked", converted.isBlocked());
        assertEquals("Last block reason", "", converted.getLastBlockReason());
        assertEquals("Class of Service", null, converted.getLocalClassOfServiceCustomField());
        assertEquals("Color", null, converted.getColor());
        assertEquals("Priority order", 0L, converted.getPriorityOrder());
    }

    private ParentProvider buildProvider() {
        return parentKey -> {
           throw new RuntimeException("Should not ask for parent during test: " + parentKey);
        };
    }

    @Test
    public void issueWithParentConvert() throws JSONException {
        when(classOfServiceDetails.getDefaultValue()).thenReturn(CLASS_OF_SERVICE_STANDARD);
        mockIssue(parent, PARENT_ISSUE_KEY, 1L);

        mockIssueField(parent, CLASS_OF_SERVICE_ID, format(JSON_CLASS_OF_SERVICE, CLASS_OF_SERVICE_EXPEDITE));
        mockIssueField(parent, RELEASE_ID, "{id: 100}");
        mockIssueField(issue, PARENT_ID, format(JSON_PARENT, PARENT_ISSUE_KEY));

        objective.taskboard.data.Issue parentIssue = subject.convertSingleIssue(parent, buildProvider());
        objective.taskboard.data.Issue childIssue = subject.convertSingleIssue(issue, parentKey -> Optional.of(parentIssue));
        childIssue.setParentCard(parentIssue);
        List<objective.taskboard.data.Issue> issuesConverted = Arrays.asList(parentIssue, childIssue);

        assertEquals("Issues converted quantity", 2, issuesConverted.size());
        objective.taskboard.data.Issue converted = issuesConverted.stream()
                .filter(i -> i.getIssueKey().equals(ISSUE_KEY))
                .findFirst().orElse(null);
        assertIssueWithParent(converted);
        assertEquals("Class of service", CLASS_OF_SERVICE_EXPEDITE, converted.getClassOfServiceValue());
        assertEquals("Release", "100", converted.getReleaseId());
    }

    @Test
    public void issueWithComment() {
        when(comment.toString()).thenReturn("comment");
        when(issue.getComments()).thenReturn(asList(comment));

        objective.taskboard.data.Issue converted = subject.convertSingleIssue(issue, buildProvider());

        assertEquals("Comment", "comment", converted.getComments());
    }

    @Test
    public void updateIssueConverted() throws JSONException {
        mockIssueField(issue, CLASS_OF_SERVICE_ID, format(JSON_CLASS_OF_SERVICE, CLASS_OF_SERVICE_EXPEDITE));

        objective.taskboard.data.Issue converted = subject.convertSingleIssue(issue, buildProvider());

        assertEquals("Class of service", converted.getClassOfServiceValue(), CLASS_OF_SERVICE_EXPEDITE);

        mockIssueField(issue, CLASS_OF_SERVICE_ID, format(JSON_CLASS_OF_SERVICE, CLASS_OF_SERVICE_STANDARD));

        objective.taskboard.data.Issue issueUpdated = subject.convertSingleIssue(issue, buildProvider());
        assertNotNull("Issue updated converted", issueUpdated);
        assertEquals("Class of service", issueUpdated.getClassOfServiceValue(), CLASS_OF_SERVICE_STANDARD);
    }
    
    @Test
    public void whenIssuesWithMultipleNestedLevelsAreReturned_makeSureValuesDependingOnParentsAreCorrectlyConverted() throws JSONException {
        JiraIssueDto A = mock(JiraIssueDto.class);
        JiraIssueDto B = mock(JiraIssueDto.class);
        JiraIssueDto C = mock(JiraIssueDto.class);
        mockIssue(A,"KEY-1");
        mockIssueField(A, PARENT_ID, format(JSON_PARENT, "PARENT-1", TYPE_ICON_URI));
        mockIssueField(A, CLASS_OF_SERVICE_ID, format(JSON_CLASS_OF_SERVICE, CLASS_OF_SERVICE_EXPEDITE));
        mockIssueField(B, PARENT_ID, format(JSON_PARENT, "PARENT-2", TYPE_ICON_URI));
        mockIssue(B,"PARENT-1");
        mockIssue(C,"PARENT-2");
        
        
        Map<String, objective.taskboard.data.Issue> issueByKey = new LinkedHashMap<>();
        ParentProvider provider = parentKey -> {
            objective.taskboard.data.Issue issue = issueByKey.get(parentKey);
            if (issue == null)
                return Optional.empty();
            
            return Optional.of(issue);
        };
        try {
            subject.convertSingleIssue(A, provider);
            fail("Should throw an IncompleteIssueException");
        }catch(IncompleteIssueException e) {
            assertEquals("PARENT-1", e.getMissingParentKey());
        }
        try {
            subject.convertSingleIssue(B, provider);
            fail("Should throw an IncompleteIssueException");
        }catch(IncompleteIssueException e) {
            assertEquals("PARENT-2", e.getMissingParentKey());
        }
        issueByKey.put("PARENT-2", subject.convertSingleIssue(C, provider));
        issueByKey.put("PARENT-1", subject.convertSingleIssue(B, provider));
        subject.convertSingleIssue(A, provider);
    }

    private void mockIssue(JiraIssueDto issue, String issueKey) {
        when(issue.getKey()).thenReturn(issueKey);
        when(issue.getProject()).thenReturn(project);
        when(issue.getIssueType()).thenReturn(issueType);
        when(issue.getStatus()).thenReturn(status);
        when(issue.getCreationDate()).thenReturn(new DateTime(0));
        when(issue.getPriority()).thenReturn(priority);
        when(issue.getWorklogs()).thenReturn(new JiraWorklogResultSetDto());
    }

    private void mockIssue(JiraIssueDto issue, String issueKey, long issueTypeId) {
        mockIssue(issue, issueKey);
        JiraIssueTypeDto issueType = mock(JiraIssueTypeDto.class);
        when(issueType.getId()).thenReturn(issueTypeId);
        when(issue.getIssueType()).thenReturn(issueType);
    }

    private void mockIssueField(JiraIssueDto issue, String fieldId, String json) throws JSONException {
        JSONObject fieldValue = new JSONObject(json);
        when(issue.getField(fieldId)).thenReturn(fieldValue);
    }

    private void assertIssueWithParent(objective.taskboard.data.Issue converted) {
        assertNotNull("Issue converted", converted);
        assertEquals("Issue key", ISSUE_KEY, converted.getIssueKey());
        assertEquals("Parent key", PARENT_ISSUE_KEY, converted.getParent());
        assertEquals("Parent type id", 1L, converted.getParentType());
        assertEquals("Parent type icon URI", TYPE_ICON_URI, converted.getParentTypeIconUri());
    }

}
