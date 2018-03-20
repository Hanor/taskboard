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

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.data.Issue;
import objective.taskboard.data.Issue.CardTeam;
import objective.taskboard.data.Team;
import objective.taskboard.data.User;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.filterConfiguration.TeamFilterConfigurationService;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.repository.TeamCachedRepository;

@Service
public class IssueTeamService {
    private static final Logger log = LoggerFactory.getLogger(IssueTeamService.class);    

    @Autowired
    private TeamFilterConfigurationService teamFilterConfigurationService;
    
    @Autowired
    private TeamCachedRepository teamRepo;
    
    @Autowired
    private ProjectFilterConfigurationCachedRepository projectRepo;
    
    public Long getDefaultTeamId(Issue issue) {
        Optional<ProjectFilterConfiguration> projectOpt = projectRepo.getProjectByKey(issue.getProjectKey());
        if (!projectOpt.isPresent()) {
            log.warn("Project not found for issue " + issue.getIssueKey() + ". This situation should be impossible");
            return null;
        }
        
        Optional<Team> team = teamRepo.findById(projectOpt.get().getDefaultTeam());
        if (!team.isPresent()) { 
            log.warn("Default team ID " + projectOpt.get().getDefaultTeam() + " for project " + issue.getProjectKey() + " not found!");
            return null;
        }
        
        return team.get().getId();
    }
    
    public Set<CardTeam> getTeamsForIds(List<Long> ids) {
        Set<CardTeam> issueTeams = new LinkedHashSet<>();
        for (Long teamId : ids) {
            Optional<Team> team = teamRepo.findById(teamId);
            if (!team.isPresent()) {
                log.warn("Invalid team id " + teamId + ". Team not found");
                continue;
            }
            issueTeams.add(CardTeam.from(team.get()));
        }
        return issueTeams;
    }
    
    public CardTeam getDefaultTeam(String projectKey) {
        Optional<ProjectFilterConfiguration> projectOpt = projectRepo.getProjectByKey(projectKey);
        Optional<Team> team = teamRepo.findById(projectOpt.get().getDefaultTeam());
        if (team.isPresent())
            return CardTeam.from(team.get());

        throw new IllegalStateException("Default team ID " + projectOpt.get().getDefaultTeam() + " for project " + projectKey + " not found!");
    }

    /**
     * Returns assigned users that are not in the issue teams 
     * @param issue
     * @return
     */
    public Set<String> getMismatchingUsers(Issue issue) {
        List<User> assignees = new LinkedList<>();
        assignees.add(issue.getAssignee());
        assignees.addAll(issue.getCoAssignees());
        
        Set<String> validTeams = issue.getTeams().stream().map(t->t.name).collect(Collectors.toSet());
        Set<String> mismatches = new LinkedHashSet<>();

        for (User user : assignees) {
            if (!user.isAssigned()) continue;

            List<Team> teams = teamFilterConfigurationService.getConfiguredTeamsByUser(user.name);
            
            if (!teams.stream().anyMatch(t -> validTeams.contains(t.getName())))
                mismatches.add(user.name);
        }

        return mismatches;    
    }
}