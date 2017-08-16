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

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import objective.taskboard.domain.converter.IssueMetadata.IssueCoAssignee;
import objective.taskboard.domain.converter.IssueTeamService.InvalidTeamException;
import objective.taskboard.filterConfiguration.TeamFilterConfigurationService;

@RunWith(MockitoJUnitRunner.class)
public class IssueTeamServiceTest {

    private static final String NAME_ASSIGNEE = "assignee";
    private static final String NAME_CO_ASSIGNEE = "co-assignee";
    private static final String NAME_PARENT_ASSIGNEE = "parent assignee";
    private static final String NAME_PARENT_CO_ASSIGNEE = "parent co-assignee";
    private static final String NAME_REPORTER = "reporter";

    private static final String MSG_USERS_SHOULD_BE_EMPTY = "Users should be empty";

    @InjectMocks
    private IssueTeamService subject;

    @Mock
    private TeamFilterConfigurationService teamFilterConfigurationService;
    @Mock
    private IssueMetadata issueMetadata;
    @Mock
    private IssueMetadata parentMetadata;
    @Mock
    private IssueCoAssignee coAssignee;

    @Before
    public void before() {
        when(teamFilterConfigurationService.getConfiguredTeamsNamesByUserAndProject(anyString(), anyString())).thenReturn(asList("Team"));
    }

    @Test
    public void issueWithValidAssignee() throws InvalidTeamException {
        when(issueMetadata.getAssignee()).thenReturn(NAME_ASSIGNEE);

        Map<String, List<String>> issueTeams = subject.getIssueTeams(issueMetadata, null);
        assertUserTeam(issueTeams, NAME_ASSIGNEE);
    }

    @Test
    public void issueWithValidCoAssignee() throws InvalidTeamException {
        when(coAssignee.getName()).thenReturn(NAME_CO_ASSIGNEE);
        when(issueMetadata.getCoAssignees()).thenReturn(asList(coAssignee));

        Map<String, List<String>> issueTeams = subject.getIssueTeams(issueMetadata, null);
        assertUserTeam(issueTeams, NAME_CO_ASSIGNEE);
    }

    @Test
    public void issueWithInvalidAssignee() {
        when(issueMetadata.getAssignee()).thenReturn(NAME_ASSIGNEE);
        when(teamFilterConfigurationService.getConfiguredTeamsNamesByUserAndProject(anyString(), anyString())).thenReturn(asList());

        try {
            subject.getIssueTeams(issueMetadata, null);
            fail();
        } catch (InvalidTeamException e) {
            assertEquals("Users in invalid team quantity", 1, e.getUsersInInvalidTeam().size());
            assertEquals("User in invalid team", NAME_ASSIGNEE, e.getUsersInInvalidTeam().get(0));
        }
    }

    @Test
    public void issueWithNoUserAndNoParent() throws InvalidTeamException {
        Map<String, List<String>> issueTeams = subject.getIssueTeams(issueMetadata, null);
        assertTrue(MSG_USERS_SHOULD_BE_EMPTY, issueTeams.isEmpty());
    }

    @Test
    public void parentIssueWithValidAssignee() throws InvalidTeamException {
        when(parentMetadata.getAssignee()).thenReturn(NAME_PARENT_ASSIGNEE);

        Map<String, List<String>> issueTeams = subject.getIssueTeams(issueMetadata, parentMetadata);
        assertUserTeam(issueTeams, NAME_PARENT_ASSIGNEE);
    }

    @Test
    public void parentIssueWithValidCoAssignee() throws InvalidTeamException {
        when(coAssignee.getName()).thenReturn(NAME_PARENT_CO_ASSIGNEE);
        when(parentMetadata.getCoAssignees()).thenReturn(asList(coAssignee));

        Map<String, List<String>> issueTeams = subject.getIssueTeams(issueMetadata, parentMetadata);
        assertUserTeam(issueTeams, NAME_PARENT_CO_ASSIGNEE);
    }

    @Test
    public void issueWithValidReporter() throws InvalidTeamException {
        when(issueMetadata.getReporter()).thenReturn(NAME_REPORTER);

        Map<String, List<String>> issueTeams = subject.getIssueTeams(issueMetadata, parentMetadata);
        assertUserTeam(issueTeams, NAME_REPORTER);
    }

    @Test
    public void issueWithReporterNull() throws InvalidTeamException {
        Map<String, List<String>> issueTeams = subject.getIssueTeams(issueMetadata, parentMetadata);
        assertTrue(MSG_USERS_SHOULD_BE_EMPTY, issueTeams.isEmpty());
    }

    private void assertUserTeam(Map<String, List<String>> issueTeams, String nameCoAssignee) {
        assertTrue("User should have been found", issueTeams.containsKey(nameCoAssignee));

        List<String> teams = issueTeams.get(nameCoAssignee);
        assertEquals("User teams quantity", 1, teams.size());
        assertEquals("User team", "Team", teams.get(0));
    }

}
