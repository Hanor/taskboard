package objective.taskboard.controller;

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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.Sets;

import objective.taskboard.data.Issue;
import objective.taskboard.data.Team;
import objective.taskboard.data.UserTeam;
import objective.taskboard.domain.Filter;
import objective.taskboard.domain.Step;
import objective.taskboard.domain.WipConfiguration;
import objective.taskboard.issueBuffer.IssueBufferService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.CustomField;
import objective.taskboard.jira.JiraProperties.CustomField.ClassOfServiceDetails;
import objective.taskboard.jira.JiraProperties.Wip;
import objective.taskboard.jira.JiraSearchService;
import objective.taskboard.jira.MetadataCachedService;
import objective.taskboard.jira.SearchIssueVisitor;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.data.Status;
import objective.taskboard.repository.TeamCachedRepository;
import objective.taskboard.repository.UserTeamCachedRepository;
import objective.taskboard.repository.WipConfigurationRepository;

@RunWith(MockitoJUnitRunner.class)
public class WipValidatorControllerTest {

    private static final String MSG_ASSERT_WIP_SHOULD_HAVE_EXCEEDED = "Wip should have exceeded";
    private static final String MSG_ASSERT_WIP_SHOULDN_T_HAVE_EXCEEDED = "Wip shouldn't have exceeded";
    private static final String MSG_ASSERT_RESPONSE_MESSAGE = "Response message";
    private static final String MSG_ASSERT_RESPONSE_STATUS_CODE = "Response status code";

    private static final String MSG_EXPECTED_WIP_EXCEEDED = "You can't exceed your team's WIP limit (Team: Team, Actual: 1, Limit: 1)";
    private static final String MSG_EXPECTED_NO_WIP = "No wip configuration was found";

    private static final Status STATUS = new Status(10L, "status", null);
    private static final String PROJECT_KEY = "PROJECT";
    private static final String USER = "user";
    private static final String TEAM_NAME = "Team";
    private static final String TEAM2_NAME = "Team2";
    private static final String CLASS_OF_SERVICE_ID = "classOfServiceId";

    @InjectMocks
    private WipValidatorController subject;

    boolean throwExceptionDuringSearch = false;
    
    @Spy
    private JiraSearchService jiraSearchService =  new JiraSearchService() {
        @Override
        public void searchIssues(String jql, SearchIssueVisitor visitor, String... additionalFields) {
            if (throwExceptionDuringSearch)
                throw new RuntimeException("Error");
            visitor.processIssue(Mockito.mock(JiraIssueDto.class));
        }
    };
    
    @Mock
    private Issue issue;
    @Mock
    private JiraProperties jiraProperties;
    @Mock
    private CustomField customField;
    @Mock
    private ClassOfServiceDetails classOfServiceDetails;
    @Mock
    private UserTeamCachedRepository userTeamRepo;
    @Mock
    private TeamCachedRepository teamRepo;
    @Mock
    private WipConfigurationRepository wipConfigRepo;
    
    @Mock
    private UserTeam userTeam;
    @Mock
    private UserTeam userTeam2;
    @Mock
    private Team team;
    @Mock
    private Team team2;
    @Mock
    private IssueBufferService cardService;

    @Mock
    private WipConfiguration wipConfig;
    @Mock
    private Step step;
    @Mock
    private MetadataCachedService metadataService;
    
    @Before
    public void before() throws JSONException {
        when(classOfServiceDetails.getId()).thenReturn(CLASS_OF_SERVICE_ID);
        when(customField.getClassOfService()).thenReturn(classOfServiceDetails);
        when(jiraProperties.getCustomfield()).thenReturn(customField);

        when(issue.getClassOfServiceValue()).thenReturn("Standard");
        when(issue.getType()).thenReturn(99L);
        when(issue.getProjectKey()).thenReturn(PROJECT_KEY);
        when(issue.getTeams()).thenReturn(Sets.newHashSet(new Issue.CardTeam(1L), new Issue.CardTeam(2L)));

        when(cardService.getIssueByKey(anyString())).thenReturn(issue);

        when(userTeam.getUserName()).thenReturn(USER);
        when(userTeam.getTeam()).thenReturn(TEAM_NAME);
        when(userTeam2.getUserName()).thenReturn(USER);
        when(userTeam2.getTeam()).thenReturn(TEAM2_NAME);
        
        when(userTeamRepo.findByUserName(USER)).thenReturn(asList(userTeam));
        when(userTeamRepo.findByTeam(TEAM_NAME)).thenReturn(asList(userTeam));
        when(userTeamRepo.findByTeam(TEAM2_NAME)).thenReturn(asList(userTeam2));

        when(team.getId()).thenReturn(1L);
        when(team.getName()).thenReturn(TEAM_NAME);
        when(teamRepo.findByName(TEAM_NAME)).thenReturn(team);
        
        when(team2.getId()).thenReturn(2L);
        when(team2.getName()).thenReturn(TEAM2_NAME);
        when(teamRepo.findByName(TEAM2_NAME)).thenReturn(team2);

        Filter filter = new Filter();
        filter.setIssueTypeId(99L);
        filter.setStatusId(STATUS.id);
        filter.setStep(step);
        
        when(step.getFilters()).thenReturn(asList(filter));

        when(wipConfig.getTeam()).thenReturn(TEAM_NAME);
        when(wipConfig.getWip()).thenReturn(1);
        when(wipConfig.isApplicable(99L, STATUS.id)).thenReturn(true);
        when(wipConfig.getStep()).thenReturn(step);
        when(wipConfigRepo.findByTeamIn(asList(TEAM_NAME))).thenReturn(asList(wipConfig));
        
        Map<Long, Status> statuses = new HashMap<>();
        statuses.put(STATUS.id, STATUS);
        when(metadataService.getStatusesMetadata()).thenReturn(statuses);
    }

    @Test
    public void wipExceeded() {
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("I-1", USER, STATUS.name);
        assertTrue(MSG_ASSERT_WIP_SHOULD_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, MSG_EXPECTED_WIP_EXCEEDED, responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, OK, responseEntity.getStatusCode());
    }

    @Test
    public void issueNotFound() {
        when(cardService.getIssueByKey(anyString())).thenReturn(null);
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("", "", "");
        assertFalse(MSG_ASSERT_WIP_SHOULDN_T_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, "Issue  not found", responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, PRECONDITION_FAILED, responseEntity.getStatusCode());
    }

    @Test
    public void userEmptyRequired() {
        when(cardService.getIssueByKey(anyString())).thenReturn(issue);
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("", "", "");
        assertFalse(MSG_ASSERT_WIP_SHOULDN_T_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, "Query parameter 'user' is required",
                responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, PRECONDITION_FAILED, responseEntity.getStatusCode());
    }

    @Test
    public void userNullRequired() {
        when(cardService.getIssueByKey(anyString())).thenReturn(issue);
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("", null, "");
        assertFalse(MSG_ASSERT_WIP_SHOULDN_T_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, "Query parameter 'user' is required",
                responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, PRECONDITION_FAILED, responseEntity.getStatusCode());
    }

    @Test
    public void statusEmptyRequired() {
        when(cardService.getIssueByKey(anyString())).thenReturn(issue);
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("", USER, "");
        assertFalse(MSG_ASSERT_WIP_SHOULDN_T_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, "Query parameter 'status' is required",
                responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, PRECONDITION_FAILED, responseEntity.getStatusCode());
    }

    @Test
    public void statusNullRequired() {
        when(cardService.getIssueByKey(anyString())).thenReturn(issue);
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("", USER, null);
        assertFalse(MSG_ASSERT_WIP_SHOULDN_T_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, "Query parameter 'status' is required",
                responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, PRECONDITION_FAILED, responseEntity.getStatusCode());
    }

    @Test
    public void classOfServiceExpedite() throws JSONException {
        when(issue.getClassOfServiceValue()).thenReturn("Expedite");
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("", USER, STATUS.name);
        assertFalse(MSG_ASSERT_WIP_SHOULDN_T_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, "Class of service is Expedite", responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, OK, responseEntity.getStatusCode());
    }

    @Test
    public void noUserTeam() {
        when(userTeamRepo.findByUserName(USER)).thenReturn(asList());
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("", USER, STATUS.name);
        assertFalse(MSG_ASSERT_WIP_SHOULDN_T_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, MSG_EXPECTED_NO_WIP, responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, OK, responseEntity.getStatusCode());
    }

    @Test
    public void noTeam() {
        when(teamRepo.findByName(TEAM_NAME)).thenReturn(null);
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("", USER, STATUS.name);
        assertFalse(MSG_ASSERT_WIP_SHOULDN_T_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, MSG_EXPECTED_NO_WIP, responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, OK, responseEntity.getStatusCode());
    }
    
    @Test
    public void standardIssueType() {
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("", USER, STATUS.name);
        assertTrue(MSG_ASSERT_WIP_SHOULD_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, MSG_EXPECTED_WIP_EXCEEDED, responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, OK, responseEntity.getStatusCode());
    }

    @Test
    public void classOfServiceFieldNull() {
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("", USER, STATUS.name);
        assertTrue(MSG_ASSERT_WIP_SHOULD_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, MSG_EXPECTED_WIP_EXCEEDED, responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, OK, responseEntity.getStatusCode());
    }

    @Test
    public void throwedNullPointerException() {
        when(userTeamRepo.findByUserName(USER)).thenReturn(null);
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("", USER, STATUS.name);
        assertFalse(MSG_ASSERT_WIP_SHOULDN_T_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, "java.lang.NullPointerException", responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    }

    @Test
    public void sortWipConfig() {
        when(userTeamRepo.findByUserName(USER)).thenReturn(asList(userTeam, userTeam2));
        
        WipConfiguration wipConfig2 = mock(WipConfiguration.class);
        when(wipConfig2.isApplicable(99L, STATUS.id)).thenReturn(true);
        when(wipConfig2.getStep()).thenReturn(step);
        when(wipConfig2.getTeam()).thenReturn(TEAM2_NAME);
        when(wipConfig2.getWip()).thenReturn(0);

        when(wipConfigRepo.findByTeamIn(Mockito.any())).thenReturn(asList(wipConfig, wipConfig2));
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("I-1", USER, STATUS.name);
        assertTrue(MSG_ASSERT_WIP_SHOULD_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE,
                "You can't exceed your team's WIP limit (Team: Team2, Actual: 1, Limit: 0)",
                responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, OK, responseEntity.getStatusCode());
    }

    @Test
    public void noWipExceeded() {
        when(wipConfig.getWip()).thenReturn(2);
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("I-1", USER, STATUS.name);
        assertFalse(MSG_ASSERT_WIP_SHOULDN_T_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, "(Team: Team, Actual: 1, Limit: 2)", responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, OK, responseEntity.getStatusCode());
    }

    @Test
    public void ignoreIssueTypesOnGetWipCountWhenPropertyIsNotEmpty() {
        Wip wip = new Wip();
        wip.setIgnoreIssuetypesIds(asList(2L, 3L));
        when(jiraProperties.getWip()).thenReturn(wip);

        subject.validate("I-1", USER, STATUS.name);
        verify(jiraSearchService).searchIssues(contains("and issuetype not in (2,3)"), anyObject());
    }

    @Test
    public void ignoreWipIfIssueHasTheSameIssueTypeFromProperty() {
        Wip wip = new Wip();
        wip.setIgnoreIssuetypesIds(asList(1L,2L));
        when(jiraProperties.getWip()).thenReturn(wip);
        when(issue.getType()).thenReturn(1L);
        when(issue.getIssueTypeName()).thenReturn("Feature Planning");

        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("I-1", USER, STATUS.name);
        assertFalse(responseEntity.getBody().isWipExceeded);
        assertEquals("Issue Type Feature Planning is ignored on WIP count.", responseEntity.getBody().message);
        assertEquals(OK, responseEntity.getStatusCode());
    }

    @Test
    public void throwedAnyException() {
        throwExceptionDuringSearch = true;
        ResponseEntity<WipValidatorResponse> responseEntity = subject.validate("I-1", USER, STATUS.name);
        assertFalse(MSG_ASSERT_WIP_SHOULDN_T_HAVE_EXCEEDED, responseEntity.getBody().isWipExceeded);
        assertEquals(MSG_ASSERT_RESPONSE_MESSAGE, "Error", responseEntity.getBody().message);
        assertEquals(MSG_ASSERT_RESPONSE_STATUS_CODE, INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    }
}
