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

import static com.atlassian.jira.rest.client.api.domain.IssueLinkType.Direction.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.atlassian.jira.rest.client.api.domain.BasicComponent;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.IssueLink;
import com.atlassian.jira.rest.client.api.domain.IssueLinkType;
import com.atlassian.jira.rest.client.api.domain.IssueType;

import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.CustomField;
import objective.taskboard.jira.JiraProperties.CustomField.Blocked;
import objective.taskboard.jira.JiraProperties.CustomField.ClassOfServiceDetails;
import objective.taskboard.jira.JiraProperties.CustomField.CustomFieldDetails;
import objective.taskboard.jira.JiraProperties.CustomField.TShirtSize;
import objective.taskboard.jira.JiraProperties.IssueType.IssueTypeDetails;

@RunWith(MockitoJUnitRunner.class)
public class IssueMetadataTest {

    private static final String PARENT_ID = "parent";
    private static final String CLASS_OF_SERVICE_ID = "classOfServiceId";
    private static final String CO_ASSIGNEES_ID = "coAssigneesId";
    private static final String BLOCKED_ID = "blockedId";
    private static final String LAST_BLOCK_REASON_ID = "lastBlockReasonId";
    private static final String ADDITIONAL_ESTIMATED_HOURS_ID = "additionalEstimatedHoursId";
    private static final String RELEASE_ID = "releaseId";
    private static final String T_SHIRT_SIZE_ID1 = "tShirtSizeId1";
    private static final String T_SHIRT_SIZE_ID2 = "tShirtSizeId2";

    private static final String MSG_REAL_PARENT_SHOULD_BE_NULL = "Real parent should be null";
    private static final String MSG_CLASS_OF_SERVICE_SHOULD_BE_NULL = "Class of service should be null";
    private static final String MSG_CO_ASSIGNEES_QUANTITY = "Co-assignees quantity";
    private static final String MSG_T_SHIRT_SIZES_QUANTITY = "T-Shirt sizes quantity";
    private static final String MSG_COMMENTS_QUANTITY = "Comments quantity";
    private static final String MSG_DEPENDENCIES_ISSUES_QUANTITY = "Dependencies issues quantity";
    private static final String MSG_RELEASE_SHOULD_BE_EMPTY = "Release should be empty";
    private static final String MSG_ASSERT_BLOCKED = "Blocked";
    private static final String MSG_ASSERT_LAST_BLOCK_REASON = "Last Block Reason";

    private static final String LINK_TYPE_NAME_DEMAND = "Demand";
    private static final String LINK_TYPE_DESC_DEMAND = "is demanded by";
    private static final String LINK_TYPE_NAME_DEPENDENCY = "Dependency";
    private static final String LINK_TYPE_DESC_DEPENDENCY = "is a dependency of";
    private static final String ISSUE_KEY = "ISSUE-1";

    @Mock
    private Issue issue;
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
    private objective.taskboard.jira.JiraProperties.IssueType issueTypeProperty;
    @Mock
    private IssueTypeDetails issueTypeDetails;
    @Mock
    private IssueType issueType;
    @Mock
    private IssueField issueField;
    @Mock
    private IssueLink issueLink;
    @Mock
    private IssueLinkType issueLinkType;
    @Mock
    private Comment comment;
    @Mock
    private Logger log;
    @Mock
    private BasicComponent basicComponent;
    @Mock
    private BasicProject basicProject;

    private void mockIssueField(String fieldId, Object fieldValue) {
        when(issueField.getValue()).thenReturn(fieldValue);
        when(issue.getField(fieldId)).thenReturn(issueField);
    }

    @Before
    public void mockJiraProperties() {
        doNothing().when(log).error(anyString());

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

        when(issueLinkProperty.getDependencies()).thenReturn(asList());
        when(jiraProperties.getIssuelink()).thenReturn(issueLinkProperty);

        when(issueTypeDetails.getId()).thenReturn(1L);
        when(issueTypeProperty.getDemand()).thenReturn(issueTypeDetails);
        when(jiraProperties.getIssuetype()).thenReturn(issueTypeProperty);

        when(issue.getIssueType()).thenReturn(issueType);
        when(issue.getProject()).thenReturn(basicProject);
    }

    @Test
    public void extractRealParentValid() throws JSONException {
        JSONObject jsonRealParent = new JSONObject("{key:'ISSUE-1', fields:{issuetype:{id:2, iconUrl:'url'}}}");
        mockIssueField(PARENT_ID, jsonRealParent);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertNotNull("Real parent shouldn't be null", metadata.getRealParent());
        assertEquals("Real parent key", ISSUE_KEY, metadata.getRealParent().getKey());
        assertEquals("Real parent issue type id", 2, metadata.getRealParent().getTypeId());
        assertEquals("Real parent issue type icon url", "url", metadata.getRealParent().getTypeIconUrl());
        assertEquals("Parent key", ISSUE_KEY, metadata.getParentKey());
    }

    @Test
    public void extractRealParentFieldValueNull() {
        mockIssueField(PARENT_ID, null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertNull(MSG_REAL_PARENT_SHOULD_BE_NULL, metadata.getRealParent());
    }

    @Test
    public void extractRealParentFieldNull() {
        when(issue.getField(PARENT_ID)).thenReturn(null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertNull(MSG_REAL_PARENT_SHOULD_BE_NULL, metadata.getRealParent());
    }

    @Test
    public void extractRealParentInvalid() throws JSONException {
        JSONObject jsonRealParent = new JSONObject("{fields:{issuetype:{id:2, iconUrl:'url'}}}");
        mockIssueField(PARENT_ID, jsonRealParent);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertNull(MSG_REAL_PARENT_SHOULD_BE_NULL, metadata.getRealParent());
    }

    @Test
    public void extractLinkedParentKeyValid() {
        when(issueLinkType.getDirection()).thenReturn(OUTBOUND);
        when(issueLinkType.getName()).thenReturn(LINK_TYPE_NAME_DEMAND);
        when(issueLinkType.getDescription()).thenReturn(LINK_TYPE_DESC_DEMAND);
        when(issueLink.getIssueLinkType()).thenReturn(issueLinkType);
        when(issueLink.getTargetIssueKey()).thenReturn(ISSUE_KEY);
        when(issue.getIssueLinks()).thenReturn(asList(issueLink));

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, asList(LINK_TYPE_DESC_DEMAND), log);

        assertEquals("Linked parent key", ISSUE_KEY, metadata.getLinkedParentKey());
        assertEquals("Parent key", ISSUE_KEY, metadata.getParentKey());
    }

    @Test
    public void extractLinkedParentKeyNull() {
        when(issueLinkType.getDirection()).thenReturn(OUTBOUND);
        when(issueLinkType.getName()).thenReturn(LINK_TYPE_NAME_DEPENDENCY);
        when(issueLinkType.getDescription()).thenReturn(LINK_TYPE_DESC_DEPENDENCY);
        when(issueLink.getIssueLinkType()).thenReturn(issueLinkType);
        when(issueLink.getTargetIssueKey()).thenReturn(ISSUE_KEY);
        when(issue.getIssueLinks()).thenReturn(asList(issueLink));

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, asList(LINK_TYPE_DESC_DEMAND), log);

        assertNull("Linked parent key should be null", metadata.getLinkedParentKey());
    }

    @Test
    public void extractLinkedParentKeyNullWhenIsDemand() {
        when(issueLinkType.getDirection()).thenReturn(OUTBOUND);
        when(issueLinkType.getName()).thenReturn(LINK_TYPE_NAME_DEMAND);
        when(issueLinkType.getDescription()).thenReturn(LINK_TYPE_DESC_DEMAND);
        when(issueLink.getIssueLinkType()).thenReturn(issueLinkType);
        when(issueLink.getTargetIssueKey()).thenReturn(ISSUE_KEY);
        when(issue.getIssueLinks()).thenReturn(asList(issueLink));
        when(issueType.getId()).thenReturn(1L);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, asList(LINK_TYPE_DESC_DEMAND), log);

        assertNull("Linked parent key should be null", metadata.getLinkedParentKey());
    }

    @Test
    public void extractCoAssigneesValid() throws JSONException {
        JSONArray jsonCoAssignees = new JSONArray("[{name:'Co-assignee 1', avatarUrls:{24x24:'avatarUrl1'}},"
                + "{name:'Co-assignee 2', avatarUrls:{24x24:'avatarUrl2'}}]");
        mockIssueField(CO_ASSIGNEES_ID, jsonCoAssignees);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_CO_ASSIGNEES_QUANTITY, 2, metadata.getCoAssignees().size());
        assertEquals("First co-assignee name", "Co-assignee 1", metadata.getCoAssignees().get(0).getName());
        assertEquals("First co-assignee avatar url", "avatarUrl1", metadata.getCoAssignees().get(0).getAvatarUrl());
        assertEquals("Second co-assignee name", "Co-assignee 2", metadata.getCoAssignees().get(1).getName());
        assertEquals("Second co-assignee avatar url", "avatarUrl2", metadata.getCoAssignees().get(1).getAvatarUrl());
    }

    @Test
    public void extractCoAssigneesFieldValueNull() {
        mockIssueField(CO_ASSIGNEES_ID, null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_CO_ASSIGNEES_QUANTITY, 0, metadata.getCoAssignees().size());
    }

    @Test
    public void extractCoAssigneesFieldNull() {
        when(issue.getField(CO_ASSIGNEES_ID)).thenReturn(null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_CO_ASSIGNEES_QUANTITY, 0, metadata.getCoAssignees().size());
    }

    @Test
    public void extractCoAssigneesInvalid() throws JSONException {
        JSONArray jsonCoAssignees = new JSONArray("[{name:'Co-assignee 1'},"
                + "{name:'Co-assignee 2', avatarUrls:{24x24:'avatarUrl2'}}]");
        mockIssueField(CO_ASSIGNEES_ID, jsonCoAssignees);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_CO_ASSIGNEES_QUANTITY, 1, metadata.getCoAssignees().size());
        assertEquals("Co-assignee name", "Co-assignee 2", metadata.getCoAssignees().get(0).getName());
        assertEquals("Co-assignee avatar url", "avatarUrl2", metadata.getCoAssignees().get(0).getAvatarUrl());
    }

    @Test
    public void extractClassOfServiceValid() throws JSONException {
        JSONObject jsonClassOfService = new JSONObject("{id:1, value:'Standard'}");
        mockIssueField(CLASS_OF_SERVICE_ID, jsonClassOfService);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertNotNull("Class of service shouldn't be null", metadata.getClassOfService());
        assertEquals("Class of service value", "Standard", metadata.getClassOfService().getValue().toString());
        assertEquals("Class of service id", 1, metadata.getClassOfService().getOptionId().longValue());
    }

    @Test
    public void extractClassOfServiceFieldValueNull() {
        mockIssueField(CLASS_OF_SERVICE_ID, null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertNull(MSG_CLASS_OF_SERVICE_SHOULD_BE_NULL, metadata.getClassOfService());
    }

    @Test
    public void extractClassOfServiceFieldNull() {
        when(issue.getField(CLASS_OF_SERVICE_ID)).thenReturn(null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertNull(MSG_CLASS_OF_SERVICE_SHOULD_BE_NULL, metadata.getClassOfService());
    }

    @Test
    public void extractClassOfServiceInvalid() throws JSONException {
        JSONObject jsonClassOfService = new JSONObject("{value:'Standard'}");
        mockIssueField(CLASS_OF_SERVICE_ID, jsonClassOfService);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertNull(MSG_CLASS_OF_SERVICE_SHOULD_BE_NULL, metadata.getClassOfService());
    }

    @Test
    public void extractBlockedValid() throws JSONException {
        JSONArray jsonBlocked = new JSONArray("[{value:'Yes'}]");
        mockIssueField(BLOCKED_ID, jsonBlocked);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_ASSERT_BLOCKED, "Yes", metadata.getBlocked());
    }

    @Test
    public void extractBlockedFieldValueNull() {
        mockIssueField(BLOCKED_ID, null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_ASSERT_BLOCKED, "", metadata.getBlocked());
    }

    @Test
    public void extractBlockedFieldNull() {
        when(issue.getField(BLOCKED_ID)).thenReturn(null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_ASSERT_BLOCKED, "", metadata.getBlocked());
    }

    @Test
    public void extractBlockedEmpty() throws JSONException {
        JSONArray jsonBlocked = new JSONArray("[]");
        mockIssueField(BLOCKED_ID, jsonBlocked);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_ASSERT_BLOCKED, "", metadata.getBlocked());
    }

    @Test
    public void extractBlockedInvalid() throws JSONException {
        JSONArray jsonBlocked = new JSONArray("[{valuee:'Yes'}]");
        mockIssueField(BLOCKED_ID, jsonBlocked);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_ASSERT_BLOCKED, "", metadata.getBlocked());
    }

    @Test
    public void extractLastBlockReasonValid() {
        mockIssueField(LAST_BLOCK_REASON_ID, "Issue blocked");

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_ASSERT_LAST_BLOCK_REASON, "Issue blocked", metadata.getLastBlockReason());
    }

    @Test
    public void extractLastBlockReasonLarge() {
        mockIssueField(LAST_BLOCK_REASON_ID, "Issue blocked                                       "
                + "                                                                                 "
                + "                                                             Issue blocked");

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_ASSERT_LAST_BLOCK_REASON, "Issue blocked                                       "
                + "                                                                                 "
                + "                                                             Issue ...", metadata.getLastBlockReason());
    }

    @Test
    public void extractLastBlockReasonFieldValueNull() {
        mockIssueField(LAST_BLOCK_REASON_ID, null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_ASSERT_LAST_BLOCK_REASON, "", metadata.getLastBlockReason());
    }

    @Test
    public void extractLastBlockReasonFieldNull() {
        when(issue.getField(LAST_BLOCK_REASON_ID)).thenReturn(null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_ASSERT_LAST_BLOCK_REASON, "", metadata.getLastBlockReason());
    }

    @Test
    public void extractTShirtSizesValid() throws JSONException {
        when(tShirtSize.getIds()).thenReturn(asList(T_SHIRT_SIZE_ID1, T_SHIRT_SIZE_ID2));
        JSONObject jsonTShirtSize = new JSONObject("{value:'M'}");
        mockIssueField(T_SHIRT_SIZE_ID1, jsonTShirtSize);
        mockIssueField(T_SHIRT_SIZE_ID2, jsonTShirtSize);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_T_SHIRT_SIZES_QUANTITY, 2, metadata.getTShirtSizes().size());
        assertEquals("T-Shirt size 1 value", "M", metadata.getTShirtSizes().get(T_SHIRT_SIZE_ID1).getValue());
        assertEquals("T-Shirt size 2 value", "M", metadata.getTShirtSizes().get(T_SHIRT_SIZE_ID2).getValue());
    }

    @Test
    public void extractTShirtSizesFieldValueNull() {
        when(tShirtSize.getIds()).thenReturn(asList(T_SHIRT_SIZE_ID1, T_SHIRT_SIZE_ID2));
        mockIssueField(T_SHIRT_SIZE_ID1, null);
        mockIssueField(T_SHIRT_SIZE_ID2, null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_T_SHIRT_SIZES_QUANTITY, 0, metadata.getTShirtSizes().size());
    }

    @Test
    public void extractTShirtSizesFieldNull() {
        when(tShirtSize.getIds()).thenReturn(asList(T_SHIRT_SIZE_ID1, T_SHIRT_SIZE_ID2));
        when(issue.getField(T_SHIRT_SIZE_ID1)).thenReturn(null);
        when(issue.getField(T_SHIRT_SIZE_ID2)).thenReturn(null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_T_SHIRT_SIZES_QUANTITY, 0, metadata.getTShirtSizes().size());
    }

    @Test
    public void extractTShirtSizesInvalid() throws JSONException {
        when(tShirtSize.getIds()).thenReturn(asList(T_SHIRT_SIZE_ID1, T_SHIRT_SIZE_ID2));
        JSONObject jsonTShirtSize = new JSONObject("{valuee:'M'}");
        mockIssueField(T_SHIRT_SIZE_ID1, jsonTShirtSize);
        mockIssueField(T_SHIRT_SIZE_ID2, jsonTShirtSize);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_T_SHIRT_SIZES_QUANTITY, 0, metadata.getTShirtSizes().size());
    }

    @Test
    public void extractCommentsValid() {
        when(comment.toString()).thenReturn("Comment");
        when(issue.getComments()).thenReturn(asList(comment));

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_COMMENTS_QUANTITY, 1, metadata.getComments().size());
        assertEquals("Comment", "Comment", metadata.getComments().get(0));
    }

    @Test
    public void extractCommentsEmpty() {
        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals(MSG_COMMENTS_QUANTITY, 0, metadata.getComments().size());
    }

    @Test
    public void extractDependenciesIssuesValid() {
        when(issueLinkProperty.getDependencies()).thenReturn(asList(LINK_TYPE_NAME_DEPENDENCY));
        when(issueLinkType.getDirection()).thenReturn(OUTBOUND);
        when(issueLinkType.getName()).thenReturn(LINK_TYPE_NAME_DEPENDENCY);
        when(issueLinkType.getDescription()).thenReturn(LINK_TYPE_DESC_DEPENDENCY);
        when(issueLink.getIssueLinkType()).thenReturn(issueLinkType);
        when(issueLink.getTargetIssueKey()).thenReturn(ISSUE_KEY);
        when(issue.getIssueLinks()).thenReturn(asList(issueLink));

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, asList(LINK_TYPE_DESC_DEMAND), log);

        assertEquals(MSG_DEPENDENCIES_ISSUES_QUANTITY, 1, metadata.getDependenciesIssuesKey().size());
        assertEquals("Dependency issue key", ISSUE_KEY, metadata.getDependenciesIssuesKey().get(0));
    }

    @Test
    public void extractDependenciesIssuesInvalidLink() {
        when(issueLinkProperty.getDependencies()).thenReturn(asList(LINK_TYPE_NAME_DEPENDENCY));
        when(issueLinkType.getDirection()).thenReturn(INBOUND);
        when(issueLinkType.getName()).thenReturn(LINK_TYPE_NAME_DEPENDENCY);
        when(issueLinkType.getDescription()).thenReturn(LINK_TYPE_DESC_DEPENDENCY);
        when(issueLink.getIssueLinkType()).thenReturn(issueLinkType);
        when(issueLink.getTargetIssueKey()).thenReturn(ISSUE_KEY);
        when(issue.getIssueLinks()).thenReturn(asList(issueLink));

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, asList(LINK_TYPE_DESC_DEMAND), log);

        assertEquals(MSG_DEPENDENCIES_ISSUES_QUANTITY, 0, metadata.getDependenciesIssuesKey().size());
    }

    @Test
    public void extractAdditionalEstimatedHoursValid() {
        mockIssueField(ADDITIONAL_ESTIMATED_HOURS_ID, 1.1);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        Map<String, objective.taskboard.data.CustomField> additionalEstimatedHours = metadata.getAdditionalEstimatedHours();
        assertTrue("Additional estimated hours should have been extracted", additionalEstimatedHours.containsKey(ADDITIONAL_ESTIMATED_HOURS_ID));

        objective.taskboard.data.CustomField cfAdditionalEstimatedHours = additionalEstimatedHours.get(ADDITIONAL_ESTIMATED_HOURS_ID);
        assertNotNull("Additional estimated hours shouldn't be null", cfAdditionalEstimatedHours);
        assertEquals("Additional estimated hours", 1.1, cfAdditionalEstimatedHours.getValue());
    }

    @Test
    public void extractAdditionalEstimatedHoursFieldValueNull() {
        mockIssueField(ADDITIONAL_ESTIMATED_HOURS_ID, null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertTrue("Additional estimated hours should be empty", metadata.getAdditionalEstimatedHours().isEmpty());
    }

    @Test
    public void extractReleaseValid() throws JSONException {
        JSONObject jsonRelease = new JSONObject("{name:RELEASE}");
        mockIssueField(RELEASE_ID, jsonRelease);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        Map<String, objective.taskboard.data.CustomField> release = metadata.getRelease();
        assertTrue("Release should have been extracted", release.containsKey(RELEASE_ID));

        objective.taskboard.data.CustomField customFieldRelease = release.get(RELEASE_ID);
        assertNotNull("Release shouldn't be null", customFieldRelease);
        assertEquals("Release", "RELEASE", customFieldRelease.getValue());
    }

    @Test
    public void extractReleaseInvalid() throws JSONException {
        JSONObject jsonRelease = new JSONObject("{namee:RELEASE}");
        mockIssueField(RELEASE_ID, jsonRelease);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertTrue(MSG_RELEASE_SHOULD_BE_EMPTY, metadata.getRelease().isEmpty());
    }

    @Test
    public void extractReleaseFieldValueNull() {
        mockIssueField(RELEASE_ID, null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertTrue(MSG_RELEASE_SHOULD_BE_EMPTY, metadata.getRelease().isEmpty());
    }

    @Test
    public void parentKeyEmpty() {
        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertEquals("Parent key", "", metadata.getParentKey());
    }

    @Test
    public void extractLabelsValid() {
        HashSet<String> labels = new HashSet<String>();
        labels.add("label1");
        labels.add("label2");
        when(issue.getLabels()).thenReturn(labels);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        Collections.sort(metadata.getLabels());
        assertEquals("Labels quantity", 2, metadata.getLabels().size());
        assertEquals("First label", "label1", metadata.getLabels().get(0));
        assertEquals("Second label", "label2", metadata.getLabels().get(1));
    }

    @Test
    public void extractLabelsNull() {
        when(issue.getLabels()).thenReturn(null);

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertTrue("Labels should be empty", metadata.getLabels().isEmpty());
    }

    @Test
    public void extractComponentsValid() {
        when(basicComponent.getName()).thenReturn("component1");
        when(issue.getComponents()).thenReturn(asList(basicComponent));

        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        Collections.sort(metadata.getComponents());
        assertEquals("Components quantity", 1, metadata.getComponents().size());
        assertEquals("Component name", "component1", metadata.getComponents().get(0));
    }

    @Test
    public void extractComponentsNull() {
        IssueMetadata metadata = new IssueMetadata(issue, jiraProperties, null, log);

        assertTrue("Components should be empty", metadata.getComponents().isEmpty());
    }

}
