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
package objective.taskboard.domain.converter;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.CustomField;
import objective.taskboard.jira.JiraProperties.CustomField.Blocked;
import objective.taskboard.jira.JiraProperties.CustomField.ClassOfServiceDetails;
import objective.taskboard.jira.JiraProperties.CustomField.CustomFieldDetails;
import objective.taskboard.jira.JiraProperties.CustomField.TShirtSize;
import objective.taskboard.jira.JiraProperties.IssueType.IssueTypeDetails;
import objective.taskboard.jira.client.JiraCommentDto;
import objective.taskboard.jira.client.JiraComponentDto;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.client.JiraIssueLinkTypeDto;
import objective.taskboard.jira.client.JiraIssueTypeDto;
import objective.taskboard.jira.client.JiraLinkDto;
import objective.taskboard.jira.client.JiraProjectDto;

@RunWith(MockitoJUnitRunner.class)
public class IssueFieldsExtractorTest {

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
    private JiraIssueDto issue;
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
    private JiraIssueTypeDto issueType;
    @Mock
    private JiraLinkDto issueLink;
    @Mock
    private JiraIssueLinkTypeDto issueLinkType;
    @Mock
    private JiraCommentDto comment;
    @Mock
    private Logger log;
    @Mock
    private JiraComponentDto basicComponent;
    @Mock
    private JiraProjectDto basicProject;

    private void mockIssueField(String fieldId, Object fieldValue) {
        when(issue.getField(fieldId)).thenReturn(fieldValue);
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

        assertNotNull("Real parent shouldn't be null", IssueFieldsExtractor.extractRealParent(issue));
        assertEquals("Real parent key", ISSUE_KEY, IssueFieldsExtractor.extractRealParent(issue));
        assertEquals("Parent key", ISSUE_KEY, IssueFieldsExtractor.extractParentKey(jiraProperties, issue, null));
    }

    @Test
    public void extractRealParentFieldValueNull() {
        mockIssueField(PARENT_ID, null);

        assertNull(MSG_REAL_PARENT_SHOULD_BE_NULL, IssueFieldsExtractor.extractRealParent(issue));
    }

    @Test
    public void extractRealParentFieldNull() {
        when(issue.getField(PARENT_ID)).thenReturn(null);

        assertNull(MSG_REAL_PARENT_SHOULD_BE_NULL, IssueFieldsExtractor.extractRealParent(issue));
    }

    @Test
    public void extractRealParentInvalid() throws JSONException {
        JSONObject jsonRealParent = new JSONObject("{fields:{issuetype:{id:2, iconUrl:'url'}}}");
        mockIssueField(PARENT_ID, jsonRealParent);

        assertNull(MSG_REAL_PARENT_SHOULD_BE_NULL, IssueFieldsExtractor.extractRealParent(issue));
    }

    @Test
    public void extractLinkedParentKeyValid() {
        when(issueLinkType.getDirection()).thenReturn(JiraIssueLinkTypeDto.Direction.OUTBOUND);
        when(issueLinkType.getName()).thenReturn(LINK_TYPE_NAME_DEMAND);
        when(issueLinkType.getDescription()).thenReturn(LINK_TYPE_DESC_DEMAND);
        when(issueLink.getIssueLinkType()).thenReturn(issueLinkType);
        when(issueLink.getTargetIssueKey()).thenReturn(ISSUE_KEY);
        when(issue.getIssueLinks()).thenReturn(asList(issueLink));

        assertEquals("Linked parent key", ISSUE_KEY, IssueFieldsExtractor.extractLinkedParentKey(jiraProperties, issue, asList(LINK_TYPE_DESC_DEMAND)));
        assertEquals("Parent key", ISSUE_KEY, IssueFieldsExtractor.extractParentKey(jiraProperties, issue, asList(LINK_TYPE_DESC_DEMAND)));
    }

    @Test
    public void extractLinkedParentKeyNull() {
        when(issueLinkType.getDirection()).thenReturn(JiraIssueLinkTypeDto.Direction.OUTBOUND);
        when(issueLinkType.getName()).thenReturn(LINK_TYPE_NAME_DEPENDENCY);
        when(issueLinkType.getDescription()).thenReturn(LINK_TYPE_DESC_DEPENDENCY);
        when(issueLink.getIssueLinkType()).thenReturn(issueLinkType);
        when(issueLink.getTargetIssueKey()).thenReturn(ISSUE_KEY);
        when(issue.getIssueLinks()).thenReturn(asList(issueLink));

        assertNull("Linked parent key should be null", IssueFieldsExtractor.extractLinkedParentKey(jiraProperties, issue, asList(LINK_TYPE_DESC_DEMAND)));
    }

    @Test
    public void extractLinkedParentKeyNullWhenIsDemand() {
        when(issueLinkType.getDirection()).thenReturn(JiraIssueLinkTypeDto.Direction.OUTBOUND);
        when(issueLinkType.getName()).thenReturn(LINK_TYPE_NAME_DEMAND);
        when(issueLinkType.getDescription()).thenReturn(LINK_TYPE_DESC_DEMAND);
        when(issueLink.getIssueLinkType()).thenReturn(issueLinkType);
        when(issueLink.getTargetIssueKey()).thenReturn(ISSUE_KEY);
        when(issue.getIssueLinks()).thenReturn(asList(issueLink));
        when(issueType.getId()).thenReturn(1L);

        assertNull("Linked parent key should be null", IssueFieldsExtractor.extractLinkedParentKey(jiraProperties, issue, asList(LINK_TYPE_DESC_DEMAND)));
    }

    @Test
    public void extractCoAssigneesValid() throws JSONException {
        JSONArray jsonCoAssignees = new JSONArray("[{name:'Co-assignee 1', avatarUrls:{24x24:'avatarUrl1'}},"
                + "{name:'Co-assignee 2', avatarUrls:{24x24:'avatarUrl2'}}]");
        mockIssueField(CO_ASSIGNEES_ID, jsonCoAssignees);

        List<IssueCoAssignee> coAssignees = IssueFieldsExtractor.extractCoAssignees(jiraProperties, issue);
        assertEquals(MSG_CO_ASSIGNEES_QUANTITY, 2, coAssignees.size());
        assertEquals("First co-assignee name", "Co-assignee 1", coAssignees.get(0).getName());
        assertEquals("First co-assignee avatar url", "avatarUrl1", coAssignees.get(0).getAvatarUrl());
        assertEquals("Second co-assignee name", "Co-assignee 2", coAssignees.get(1).getName());
        assertEquals("Second co-assignee avatar url", "avatarUrl2", coAssignees.get(1).getAvatarUrl());
    }

    @Test
    public void extractCoAssigneesFieldValueNull() {
        mockIssueField(CO_ASSIGNEES_ID, null);

        assertEquals(MSG_CO_ASSIGNEES_QUANTITY, 0, IssueFieldsExtractor.extractCoAssignees(jiraProperties, issue).size());
    }

    @Test
    public void extractCoAssigneesFieldNull() {
        when(issue.getField(CO_ASSIGNEES_ID)).thenReturn(null);

        assertEquals(MSG_CO_ASSIGNEES_QUANTITY, 0, IssueFieldsExtractor.extractCoAssignees(jiraProperties, issue).size());
    }

    @Test
    public void extractCoAssigneesInvalid() throws JSONException {
        JSONArray jsonCoAssignees = new JSONArray("[{name:'Co-assignee 1'},"
                + "{name:'Co-assignee 2', avatarUrls:{24x24:'avatarUrl2'}}]");
        mockIssueField(CO_ASSIGNEES_ID, jsonCoAssignees);

        List<IssueCoAssignee> coAssignees = IssueFieldsExtractor.extractCoAssignees(jiraProperties, issue);
        assertEquals(MSG_CO_ASSIGNEES_QUANTITY, 1, coAssignees.size());
        assertEquals("Co-assignee name", "Co-assignee 2", coAssignees.get(0).getName());
        assertEquals("Co-assignee avatar url", "avatarUrl2", coAssignees.get(0).getAvatarUrl());
    }

    @Test
    public void extractClassOfServiceValid() throws JSONException {
        JSONObject jsonClassOfService = new JSONObject("{id:1, value:'Standard'}");
        mockIssueField(CLASS_OF_SERVICE_ID, jsonClassOfService);

        objective.taskboard.data.CustomField classOfService = IssueFieldsExtractor.extractClassOfService(jiraProperties, issue);
        assertNotNull("Class of service shouldn't be null", classOfService);
        assertEquals("Class of service value", "Standard", classOfService.getValue().toString());
        assertEquals("Class of service id", 1, classOfService.getOptionId().longValue());
    }

    @Test
    public void extractClassOfServiceFieldValueNull() {
        mockIssueField(CLASS_OF_SERVICE_ID, null);

        assertNull(MSG_CLASS_OF_SERVICE_SHOULD_BE_NULL, IssueFieldsExtractor.extractClassOfService(jiraProperties, issue));
    }

    @Test
    public void extractClassOfServiceFieldNull() {
        when(issue.getField(CLASS_OF_SERVICE_ID)).thenReturn(null);

        assertNull(MSG_CLASS_OF_SERVICE_SHOULD_BE_NULL, IssueFieldsExtractor.extractClassOfService(jiraProperties, issue));
    }

    @Test
    public void extractClassOfServiceInvalid() throws JSONException {
        JSONObject jsonClassOfService = new JSONObject("{value:'Standard'}");
        mockIssueField(CLASS_OF_SERVICE_ID, jsonClassOfService);

        assertNull(MSG_CLASS_OF_SERVICE_SHOULD_BE_NULL, IssueFieldsExtractor.extractClassOfService(jiraProperties, issue));
    }

    @Test
    public void extractBlockedValid() throws JSONException {
        JSONArray jsonBlocked = new JSONArray("[{id:'0'}]");
        mockIssueField(BLOCKED_ID, jsonBlocked);

        assertTrue(MSG_ASSERT_BLOCKED, IssueFieldsExtractor.extractBlocked(jiraProperties, issue));
    }

    @Test
    public void extractBlockedFieldValueNull() {
        mockIssueField(BLOCKED_ID, null);

        assertFalse(MSG_ASSERT_BLOCKED, IssueFieldsExtractor.extractBlocked(jiraProperties, issue));
    }

    @Test
    public void extractBlockedFieldNull() {
        when(issue.getField(BLOCKED_ID)).thenReturn(null);

        assertFalse(MSG_ASSERT_BLOCKED, IssueFieldsExtractor.extractBlocked(jiraProperties, issue));
    }

    @Test
    public void extractBlockedEmpty() throws JSONException {
        JSONArray jsonBlocked = new JSONArray("[]");
        mockIssueField(BLOCKED_ID, jsonBlocked);

        assertFalse(MSG_ASSERT_BLOCKED, IssueFieldsExtractor.extractBlocked(jiraProperties, issue));
    }

    @Test
    public void extractBlockedInvalid() throws JSONException {
        JSONArray jsonBlocked = new JSONArray("[{valuee:'Yes'}]");
        mockIssueField(BLOCKED_ID, jsonBlocked);

        assertFalse(MSG_ASSERT_BLOCKED, IssueFieldsExtractor.extractBlocked(jiraProperties, issue));
    }

    @Test
    public void extractLastBlockReasonValid() {
        mockIssueField(LAST_BLOCK_REASON_ID, "Issue blocked");

        assertEquals(MSG_ASSERT_LAST_BLOCK_REASON, "Issue blocked", IssueFieldsExtractor.extractLastBlockReason(jiraProperties, issue));
    }

    @Test
    public void extractLastBlockReasonLarge() {
        mockIssueField(LAST_BLOCK_REASON_ID, "Issue blocked                                       "
                + "                                                                                 "
                + "                                                             Issue blocked");

        assertEquals(MSG_ASSERT_LAST_BLOCK_REASON, "Issue blocked                                       "
                + "                                                                                 "
                + "                                                             Issue ...", IssueFieldsExtractor.extractLastBlockReason(jiraProperties, issue));
    }

    @Test
    public void extractLastBlockReasonFieldValueNull() {
        mockIssueField(LAST_BLOCK_REASON_ID, null);

        assertEquals(MSG_ASSERT_LAST_BLOCK_REASON, "", IssueFieldsExtractor.extractLastBlockReason(jiraProperties, issue));
    }

    @Test
    public void extractLastBlockReasonFieldNull() {
        when(issue.getField(LAST_BLOCK_REASON_ID)).thenReturn(null);

        assertEquals(MSG_ASSERT_LAST_BLOCK_REASON, "", IssueFieldsExtractor.extractLastBlockReason(jiraProperties, issue));
    }

    @Test
    public void extractTShirtSizesValid() throws JSONException {
        when(tShirtSize.getIds()).thenReturn(asList(T_SHIRT_SIZE_ID1, T_SHIRT_SIZE_ID2));
        JSONObject jsonTShirtSize = new JSONObject("{value:'M'}");
        mockIssueField(T_SHIRT_SIZE_ID1, jsonTShirtSize);
        mockIssueField(T_SHIRT_SIZE_ID2, jsonTShirtSize);

        Map<String, objective.taskboard.data.CustomField> tShirtSizes = IssueFieldsExtractor.extractTShirtSizes(jiraProperties, issue);
        assertEquals(MSG_T_SHIRT_SIZES_QUANTITY, 2, tShirtSizes.size());
        assertEquals("T-Shirt size 1 value", "M", tShirtSizes.get(T_SHIRT_SIZE_ID1).getValue());
        assertEquals("T-Shirt size 2 value", "M", tShirtSizes.get(T_SHIRT_SIZE_ID2).getValue());
    }

    @Test
    public void extractTShirtSizesFieldValueNull() {
        when(tShirtSize.getIds()).thenReturn(asList(T_SHIRT_SIZE_ID1, T_SHIRT_SIZE_ID2));
        mockIssueField(T_SHIRT_SIZE_ID1, null);
        mockIssueField(T_SHIRT_SIZE_ID2, null);

        assertEquals(MSG_T_SHIRT_SIZES_QUANTITY, 0, IssueFieldsExtractor.extractTShirtSizes(jiraProperties, issue).size());
    }

    @Test
    public void extractTShirtSizesFieldNull() {
        when(tShirtSize.getIds()).thenReturn(asList(T_SHIRT_SIZE_ID1, T_SHIRT_SIZE_ID2));
        when(issue.getField(T_SHIRT_SIZE_ID1)).thenReturn(null);
        when(issue.getField(T_SHIRT_SIZE_ID2)).thenReturn(null);

        assertEquals(MSG_T_SHIRT_SIZES_QUANTITY, 0, IssueFieldsExtractor.extractTShirtSizes(jiraProperties, issue).size());
    }

    @Test
    public void extractTShirtSizesInvalid() throws JSONException {
        when(tShirtSize.getIds()).thenReturn(asList(T_SHIRT_SIZE_ID1, T_SHIRT_SIZE_ID2));
        JSONObject jsonTShirtSize = new JSONObject("{valuee:'M'}");
        mockIssueField(T_SHIRT_SIZE_ID1, jsonTShirtSize);
        mockIssueField(T_SHIRT_SIZE_ID2, jsonTShirtSize);

        assertEquals(MSG_T_SHIRT_SIZES_QUANTITY, 0, IssueFieldsExtractor.extractTShirtSizes(jiraProperties, issue).size());
    }

    @Test
    public void extractCommentsValid() {
        when(comment.toString()).thenReturn("Comment");
        when(issue.getComments()).thenReturn(asList(comment));

        List<String> comments = IssueFieldsExtractor.extractComments(issue);
        assertEquals(MSG_COMMENTS_QUANTITY, 1, comments.size());
        assertEquals("Comment", "Comment", comments.get(0));
    }

    @Test
    public void extractCommentsEmpty() {
        assertEquals(MSG_COMMENTS_QUANTITY, 0, IssueFieldsExtractor.extractComments(issue).size());
    }

    @Test
    public void extractDependenciesIssuesValid() {
        when(issueLinkProperty.getDependencies()).thenReturn(asList(LINK_TYPE_NAME_DEPENDENCY));
        when(issueLinkType.getDirection()).thenReturn(JiraIssueLinkTypeDto.Direction.OUTBOUND);
        when(issueLinkType.getName()).thenReturn(LINK_TYPE_NAME_DEPENDENCY);
        when(issueLinkType.getDescription()).thenReturn(LINK_TYPE_DESC_DEPENDENCY);
        when(issueLink.getIssueLinkType()).thenReturn(issueLinkType);
        when(issueLink.getTargetIssueKey()).thenReturn(ISSUE_KEY);
        when(issue.getIssueLinks()).thenReturn(asList(issueLink));

        List<String> dependenciesIssuesKey = IssueFieldsExtractor.extractDependenciesIssues(jiraProperties, issue);
        assertEquals(MSG_DEPENDENCIES_ISSUES_QUANTITY, 1, dependenciesIssuesKey.size());
        assertEquals("Dependency issue key", ISSUE_KEY, dependenciesIssuesKey.get(0));
    }

    @Test
    public void extractDependenciesIssuesInvalidLink() {
        when(issueLinkProperty.getDependencies()).thenReturn(asList(LINK_TYPE_NAME_DEPENDENCY));
        when(issueLinkType.getDirection()).thenReturn(JiraIssueLinkTypeDto.Direction.INBOUND);
        when(issueLinkType.getName()).thenReturn(LINK_TYPE_NAME_DEPENDENCY);
        when(issueLinkType.getDescription()).thenReturn(LINK_TYPE_DESC_DEPENDENCY);
        when(issueLink.getIssueLinkType()).thenReturn(issueLinkType);
        when(issueLink.getTargetIssueKey()).thenReturn(ISSUE_KEY);
        when(issue.getIssueLinks()).thenReturn(asList(issueLink));

        assertEquals(MSG_DEPENDENCIES_ISSUES_QUANTITY, 0, IssueFieldsExtractor.extractDependenciesIssues(jiraProperties, issue).size());
    }

    @Test
    public void extractAdditionalEstimatedHoursValid() {
        mockIssueField(ADDITIONAL_ESTIMATED_HOURS_ID, 1.1);

        objective.taskboard.data.CustomField additionalEstimatedHours = IssueFieldsExtractor.extractAdditionalEstimatedHours(jiraProperties, issue);
        assertNotNull("Additional estimated hours shouldn't be null", additionalEstimatedHours);
        assertEquals("Additional estimated hours", 1.1, additionalEstimatedHours.getValue());
    }

    @Test
    public void extractAdditionalEstimatedHoursFieldValueNull() {
        mockIssueField(ADDITIONAL_ESTIMATED_HOURS_ID, null);

        assertNull("Additional estimated hours should be null", IssueFieldsExtractor.extractAdditionalEstimatedHours(jiraProperties, issue));
    }

    @Test
    public void extractReleaseValid() throws JSONException {
        JSONObject jsonRelease = new JSONObject("{id:10000, name:RELEASE}");
        mockIssueField(RELEASE_ID, jsonRelease);

        String release = IssueFieldsExtractor.extractReleaseId(jiraProperties, issue);
        assertThat("Release should have been extracted", release, is("10000"));
    }

    @Test
    public void extractReleaseInvalid() throws JSONException {
        JSONObject jsonRelease = new JSONObject("{namee:RELEASE}");
        mockIssueField(RELEASE_ID, jsonRelease);

        assertThat(MSG_RELEASE_SHOULD_BE_EMPTY, IssueFieldsExtractor.extractReleaseId(jiraProperties, issue), is(nullValue()));
    }

    @Test
    public void extractReleaseFieldValueNull() {
        mockIssueField(RELEASE_ID, null);

        assertThat(MSG_RELEASE_SHOULD_BE_EMPTY, IssueFieldsExtractor.extractReleaseId(jiraProperties, issue), is(nullValue()));
    }

    @Test
    public void parentKeyEmpty() {
        assertEquals("Parent key", "", IssueFieldsExtractor.extractParentKey(jiraProperties, issue, null));
    }

    @Test
    public void extractLabelsValid() {
        HashSet<String> labels = new HashSet<String>();
        labels.add("label1");
        labels.add("label2");
        when(issue.getLabels()).thenReturn(labels);

        List<String> extractedLabels = IssueFieldsExtractor.extractLabels(issue);
        Collections.sort(extractedLabels);
        assertEquals("Labels quantity", 2, extractedLabels.size());
        assertEquals("First label", "label1", extractedLabels.get(0));
        assertEquals("Second label", "label2", extractedLabels.get(1));
    }

    @Test
    public void extractLabelsNull() {
        when(issue.getLabels()).thenReturn(null);

        assertTrue("Labels should be empty", IssueFieldsExtractor.extractLabels(issue).isEmpty());
    }

    @Test
    public void extractComponentsValid() {
        when(basicComponent.getName()).thenReturn("component1");
        when(issue.getComponents()).thenReturn(asList(basicComponent));

        List<String> components = IssueFieldsExtractor.extractComponents(issue);
        Collections.sort(components);
        assertEquals("Components quantity", 1, components.size());
        assertEquals("Component name", "component1", components.get(0));
    }

    @Test
    public void extractComponentsNull() {
        assertTrue("Components should be empty", IssueFieldsExtractor.extractComponents(issue).isEmpty());
    }
}
