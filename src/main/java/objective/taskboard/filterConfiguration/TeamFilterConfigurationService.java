package objective.taskboard.filterConfiguration;

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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import objective.taskboard.config.CacheConfiguration;
import objective.taskboard.config.LoggedInUserKeyGenerator;
import objective.taskboard.data.Team;
import objective.taskboard.domain.TeamFilterConfiguration;
import objective.taskboard.jira.ProjectService;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.repository.TeamCachedRepository;
import objective.taskboard.repository.TeamFilterConfigurationCachedRepository;
import objective.taskboard.repository.UserTeamCachedRepository;

@Service
public class TeamFilterConfigurationService {

    @Autowired
    private TeamFilterConfigurationCachedRepository teamFilterConfigurationRepository;

    @Autowired
    private TeamCachedRepository teamRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectFilterConfigurationCachedRepository projectFilterConfiguration;

    @Autowired
    private UserTeamCachedRepository userTeamRepository;

    @Cacheable(CacheConfiguration.CONFIGURED_TEAMS)
    private List<Team> getConfiguredTeams() {
        Set<Long> configuredTeamsIds = teamFilterConfigurationRepository.getCache()
                .stream()
                .map(TeamFilterConfiguration::getTeamId)
                .collect(toSet());

        return getTeamsByIds(configuredTeamsIds);
    }

    @Cacheable(cacheNames = CacheConfiguration.TEAMS_VISIBLE_TO_USER, keyGenerator = LoggedInUserKeyGenerator.NAME)
    public List<Team> getDefaultTeamsInProjectsVisibleToUser() {
        Set<Long> visibleTeamsIds = projectService.getNonArchivedJiraProjectsForUser()
                .stream()
                .flatMap(p -> p.getTeamsIds().stream())
                .collect(toSet());

        return getTeamsByIds(visibleTeamsIds);
    }

    private List<Team> getTeamsByIds(Set<Long> teamsIds) {
        return teamRepository.getCache()
                .stream()
                .filter(t -> teamsIds.contains(t.getId()))
                .collect(toList());
    }

    public List<String> getConfiguredTeamsNamesByUserAndProject(String user, String projectKey) {
        Set<Long> teamsIdsProject = projectFilterConfiguration.getProjects()
            .stream()
            .filter(pf -> Objects.equals(pf.getProjectKey(), projectKey))
            .map(pf -> pf.getDefaultTeam())
            .collect(toSet());

        return getConfiguredTeamsByUser(user).stream()
                .filter(t -> teamsIdsProject.contains(t.getId()))
                .map(t -> t.getName())
                .collect(toList());
    }

    public List<Team> getConfiguredTeamsByUser(String user) {
        return userTeamRepository.findByUserName(user)
                .stream()
                .map(ut -> getConfiguredTeamByName(ut.getTeam()))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private Team getConfiguredTeamByName(String teamName) {
        return getConfiguredTeams().stream()
                .filter(t -> Objects.equals(t.getName(), teamName))
                .findFirst()
                .orElse(null);
    }
}
