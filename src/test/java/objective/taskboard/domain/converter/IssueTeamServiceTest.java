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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import objective.taskboard.data.Issue;
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
    
    private Issue issue = new Issue();
    
    
    private Issue parentCard = new Issue();
    @Mock
    private IssueCoAssignee coAssignee;

    @Before
    public void before() {
        when(teamFilterConfigurationService.getConfiguredTeamsNamesByUserAndProject(anyString(), anyString())).thenReturn(asList("Team"));
        issue.setIssueKey("FOO-34");
    }

    @Test
    public void issueWithValidAssignee() throws InvalidTeamException {
        issue.setAssignee(NAME_ASSIGNEE);

        Map<String, List<String>> issueTeams = subject.getIssueTeams(issue);
        assertUserTeam(issueTeams, NAME_ASSIGNEE);
    }

    @Test
    public void issueWithValidCoAssignee() throws InvalidTeamException {
        when(coAssignee.getName()).thenReturn(NAME_CO_ASSIGNEE);
        issue.setCoAssignees(asList(coAssignee));

        Map<String, List<String>> issueTeams = subject.getIssueTeams(issue);
        assertUserTeam(issueTeams, NAME_CO_ASSIGNEE);
    }

    @Test
    public void issueWithInvalidAssignee() {
        issue.setAssignee(NAME_ASSIGNEE);
        when(teamFilterConfigurationService.getConfiguredTeamsNamesByUserAndProject(anyString(), anyString())).thenReturn(asList());

        try {
            subject.getIssueTeams(issue);
            fail();
        } catch (InvalidTeamException e) {
            assertEquals("Users in invalid team quantity", 1, e.getUsersInInvalidTeam().size());
            assertEquals("User in invalid team", NAME_ASSIGNEE, e.getUsersInInvalidTeam().get(0));
        }
    }

    @Test
    public void issueWithNoUserAndNoParent() throws InvalidTeamException {
        Map<String, List<String>> issueTeams = subject.getIssueTeams(issue);
        assertTrue(MSG_USERS_SHOULD_BE_EMPTY, issueTeams.isEmpty());
    }

    @Test
    public void parentIssueWithValidAssignee() throws InvalidTeamException {
        parentCard.setAssignee(NAME_PARENT_ASSIGNEE);
        issue.setParentCard(parentCard);

        Map<String, List<String>> issueTeams = subject.getIssueTeams(issue);
        assertUserTeam(issueTeams, NAME_PARENT_ASSIGNEE);
    }

    @Test
    public void parentIssueWithValidCoAssignee() throws InvalidTeamException {
        when(coAssignee.getName()).thenReturn(NAME_PARENT_CO_ASSIGNEE);
        parentCard.setCoAssignees(asList(coAssignee));
        issue.setParentCard(parentCard);

        Map<String, List<String>> issueTeams = subject.getIssueTeams(issue);
        assertUserTeam(issueTeams, NAME_PARENT_CO_ASSIGNEE);
    }

    @Test
    public void issueWithValidReporter() throws InvalidTeamException {
        issue.setReporter(NAME_REPORTER);
        issue.setParentCard(parentCard);

        Map<String, List<String>> issueTeams = subject.getIssueTeams(issue);
        assertUserTeam(issueTeams, NAME_REPORTER);
    }

    @Test
    public void issueWithReporterNull() throws InvalidTeamException {
        issue.setParentCard(parentCard);
        Map<String, List<String>> issueTeams = subject.getIssueTeams(issue);
        assertTrue(MSG_USERS_SHOULD_BE_EMPTY, issueTeams.isEmpty());
    }

    private void assertUserTeam(Map<String, List<String>> issueTeams, String nameCoAssignee) {
        assertTrue("User should have been found", issueTeams.containsKey(nameCoAssignee));

        List<String> teams = issueTeams.get(nameCoAssignee);
        assertEquals("User teams quantity", 1, teams.size());
        assertEquals("User team", "Team", teams.get(0));
    }

}
