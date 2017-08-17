package objective.taskboard.domain.converter;

/*-
 * [LICENSE]
 * Taskboard
 * ---
 * Copyright (C) 2015 - 2017 Objective Solutions
 * ---
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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.sql.Date;
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
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.jira.rest.client.api.domain.BasicPriority;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Status;
import com.atlassian.jira.rest.client.api.domain.User;

import objective.taskboard.database.IssuePriorityService;
import objective.taskboard.domain.IssueColorService;
import objective.taskboard.domain.converter.IssueTeamService.InvalidTeamException;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.CustomField;
import objective.taskboard.jira.JiraProperties.CustomField.Blocked;
import objective.taskboard.jira.JiraProperties.CustomField.ClassOfServiceDetails;
import objective.taskboard.jira.JiraProperties.CustomField.CustomFieldDetails;
import objective.taskboard.jira.JiraProperties.CustomField.TShirtSize;
import objective.taskboard.jira.JiraService;
import objective.taskboard.repository.ParentIssueLinkRepository;

@RunWith(MockitoJUnitRunner.class)
public class JiraIssueToIssueConverterTest {

    private static final String PARENT_ISSUE_KEY = "ISSUE-1";
    private static final String ISSUE_KEY = "ISSUE-2";
    private static final String TYPE_ICON_URI = "iconURI";
    private static final String CLASS_OF_SERVICE_EXPEDITE = "Expedite";
    private static final String CLASS_OF_SERVICE_STANDARD = "Standard";
    private static final String RELEASE = "RELEASE";

    private static final String JSON_PARENT = "{key:'%s', fields:{issuetype:{id:1, iconUrl:'%s'}}}";
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
    private Issue issue;
    @Mock
    private Issue parent;
    @Mock
    private BasicProject project;
    @Mock
    private BasicPriority priority;
    @Mock
    private IssueType issueType;
    @Mock
    private Status status;
    @Mock
    private User assignee;
    @Mock
    private IssueMetadata issueMetadata;
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
    private IssueField classOfServiceField;
    @Mock
    private IssueField releaseField;
    @Mock
    private IssueField parentField;
    @Mock
    private IssuePriorityService priorityService;
    @Mock
    private Comment comment;
    
    private Map<String, IssueMetadata> metadatasByIssueKey = newHashMap();

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

        when(jiraProperties.getCustomfield()).thenReturn(customField);

        when(issueType.getIconUri()).thenReturn(URI.create(TYPE_ICON_URI));
        mockIssue(issue, ISSUE_KEY);

        when(priorityService.determinePriority(any())).thenReturn(0L);
        
        when(priorityService.priorityUpdateDate(any())).thenReturn(Optional.empty());
    }

    @Test
    public void simpleIssueConvert() throws InvalidTeamException {
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

        Map<String, List<String>> usersTeam = newHashMap();
        usersTeam.put("assignee", asList("team"));
        when(issueTeamService.getIssueTeams(any(), any())).thenReturn(usersTeam);

        List<objective.taskboard.data.Issue> issuesConverted = subject.convertIssues(newArrayList(issue), metadatasByIssueKey);

        assertEquals("Issues converted quantity", 1, issuesConverted.size());
        objective.taskboard.data.Issue converted = issuesConverted.get(0);
        assertEquals("Issue key", ISSUE_KEY, converted.getIssueKey());
        assertEquals("Project key", "ISSUE", converted.getProjectKey());
        assertEquals("Project name", "PROJECT", converted.getProject());
        assertEquals("Issue type id", 1L, converted.getType());
        assertEquals("Issue type icon URI", TYPE_ICON_URI, converted.getTypeIconUri());
        assertEquals("Summary", "Summary", converted.getSummary());
        assertEquals("Status id", 1L, converted.getStatus());
        assertEquals("Start date step millis", 1L, converted.getStartDateStepMillis());
        assertEquals("Assignee avatar URI", "assigneeAvatarURI", converted.getSubresponsavel1());
        assertEquals("Co-assignee avatar URI", "", converted.getSubresponsavel2());
        assertEquals("Parent key", "", converted.getParent());
        assertEquals("Parent type id", 0L, converted.getParentType());
        assertEquals("Parent type icon URI", "", converted.getParentTypeIconUri());
        assertEquals("Dependencies quantity", 0, converted.getDependencies().size());
        assertEquals("Co-assignees", "", converted.getSubResponsaveis());
        assertEquals("Assignee name", "assignee", converted.getAssignee());
        assertEquals("Users team", "assignee", converted.getUsersTeam());
        assertEquals("Priority", 1L, converted.getPriority());
        assertEquals("Due date", new Date(0), converted.getDueDate());
        assertEquals("Creation date millis", new DateTime(0).getMillis(), converted.getCreated());
        assertEquals("Description", "Description", converted.getDescription());
        assertEquals("Teams", 1, converted.getTeams().size());
        assertEquals("Team name", "team", converted.getTeams().get(0));
        assertEquals("Comments", "", converted.getComments());
        Map<String, Object> customFields = converted.getCustomFields();
        assertTrue("Class of service should be in custom fields", customFields.containsKey(CLASS_OF_SERVICE_ID));
        assertTrue("Blocked should be in custom fields", customFields.containsKey(BLOCKED_ID));
        assertTrue("Last block reason should be in custom fields", customFields.containsKey(LAST_BLOCK_REASON_ID));
        assertEquals("Color", null, converted.getColor());
        assertEquals("Priority order", 0L, converted.getPriorityOrder().longValue());
    }

    @Test
    public void issueWithParentConvert() throws JSONException {
        when(classOfServiceDetails.getDefaultValue()).thenReturn(CLASS_OF_SERVICE_STANDARD);
        mockIssue(parent, PARENT_ISSUE_KEY);

        mockIssueField(parent, classOfServiceField, CLASS_OF_SERVICE_ID, format(JSON_CLASS_OF_SERVICE, CLASS_OF_SERVICE_EXPEDITE));
        mockIssueField(parent, releaseField, RELEASE_ID, format("{name:%s}", RELEASE));
        mockIssueField(issue, parentField, PARENT_ID, format(JSON_PARENT, PARENT_ISSUE_KEY, TYPE_ICON_URI));

        List<objective.taskboard.data.Issue> issuesConverted = subject.convertIssues(newArrayList(parent, issue), metadatasByIssueKey);

        assertEquals("Issues converted quantity", 2, issuesConverted.size());
        objective.taskboard.data.Issue converted = issuesConverted.stream()
                .filter(i -> i.getIssueKey().equals(ISSUE_KEY))
                .findFirst().orElse(null);
        assertIssueWithParent(converted);
        Map<String, Object> customFields = converted.getCustomFields();
        assertClassOfService(customFields, CLASS_OF_SERVICE_EXPEDITE);
        assertTrue("Release should be in custom fields", customFields.containsKey(RELEASE_ID));
        assertEquals("Release value", RELEASE, ((objective.taskboard.data.CustomField)customFields.get(RELEASE_ID)).getValue());
    }

    @Test
    public void callJiraServiceToGetParentConvert() throws JSONException {
        when(jiraService.getIssueByKeyAsMaster(anyString())).thenReturn(parent);

        mockIssue(parent, PARENT_ISSUE_KEY);

        mockIssueField(parent, classOfServiceField, CLASS_OF_SERVICE_ID, format(JSON_CLASS_OF_SERVICE, CLASS_OF_SERVICE_EXPEDITE));
        mockIssueField(issue, parentField, PARENT_ID, format(JSON_PARENT, PARENT_ISSUE_KEY, TYPE_ICON_URI));

        List<objective.taskboard.data.Issue> issuesConverted = subject.convertIssues(newArrayList(issue), metadatasByIssueKey);

        assertEquals("Issues converted quantity", 1, issuesConverted.size());
        objective.taskboard.data.Issue converted = issuesConverted.get(0);
        assertIssueWithParent(converted);
        assertClassOfService(converted.getCustomFields(), CLASS_OF_SERVICE_EXPEDITE);
    }

    @Test
    public void issueWithComment() {
        when(comment.toString()).thenReturn("comment");
        when(issue.getComments()).thenReturn(asList(comment));

        List<objective.taskboard.data.Issue> issuesConverted = subject.convertIssues(newArrayList(issue), metadatasByIssueKey);

        assertEquals("Issues converted quantity", 1, issuesConverted.size());
        objective.taskboard.data.Issue converted = issuesConverted.get(0);
        assertEquals("Comment", "comment", converted.getComments());
    }

    @Test
    public void updateIssueConverted() throws JSONException {
        mockIssueField(issue, classOfServiceField, CLASS_OF_SERVICE_ID, format(JSON_CLASS_OF_SERVICE, CLASS_OF_SERVICE_EXPEDITE));

        List<objective.taskboard.data.Issue> issuesConverted = subject.convertIssues(newArrayList(issue), metadatasByIssueKey);

        assertEquals("Issues converted quantity", 1, issuesConverted.size());
        objective.taskboard.data.Issue converted = issuesConverted.get(0);
        assertClassOfService(converted.getCustomFields(), CLASS_OF_SERVICE_EXPEDITE);

        mockIssueField(issue, classOfServiceField, CLASS_OF_SERVICE_ID, format(JSON_CLASS_OF_SERVICE, CLASS_OF_SERVICE_STANDARD));

        objective.taskboard.data.Issue issueUpdated = subject.convertSingleIssue(issue, metadatasByIssueKey);
        assertNotNull("Issue updated converted", issueUpdated);
        assertClassOfService(issueUpdated.getCustomFields(), CLASS_OF_SERVICE_STANDARD);
    }

    private void mockIssue(Issue issue, String issueKey) {
        when(issue.getKey()).thenReturn(issueKey);
        when(issue.getProject()).thenReturn(project);
        when(issue.getIssueType()).thenReturn(issueType);
        when(issue.getStatus()).thenReturn(status);
        when(issue.getCreationDate()).thenReturn(new DateTime(0));
        when(issue.getPriority()).thenReturn(priority);
    }

    private void mockIssueField(Issue issue, IssueField issueField, String fieldId, String json) throws JSONException {
        JSONObject fieldValue = new JSONObject(json);
        when(issueField.getValue()).thenReturn(fieldValue);
        when(issue.getField(fieldId)).thenReturn(issueField);
    }

    private void assertIssueWithParent(objective.taskboard.data.Issue converted) {
        assertNotNull("Issue converted", converted);
        assertEquals("Issue key", ISSUE_KEY, converted.getIssueKey());
        assertEquals("Parent key", PARENT_ISSUE_KEY, converted.getParent());
        assertEquals("Parent type id", 1L, converted.getParentType());
        assertEquals("Parent type icon URI", TYPE_ICON_URI, converted.getParentTypeIconUri());
    }

    private void assertClassOfService(Map<String, Object> customFields, String classOfServiceExpected) {
        assertTrue("Class of service should be in custom fields", customFields.containsKey(CLASS_OF_SERVICE_ID));
        assertEquals("Class of service value", classOfServiceExpected, customFields.get(CLASS_OF_SERVICE_ID));
    }

}
