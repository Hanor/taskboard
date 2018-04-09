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
package objective.taskboard.task;

import static java.lang.String.format;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import objective.taskboard.data.Team;
import objective.taskboard.data.UserTeam;
import objective.taskboard.domain.Filter;
import objective.taskboard.domain.IssueTypeConfiguration;
import objective.taskboard.domain.Lane;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.domain.TeamFilterConfiguration;
import objective.taskboard.repository.FilterCachedRepository;
import objective.taskboard.repository.IssueTypeConfigurationCachedRepository;
import objective.taskboard.repository.LaneCachedRepository;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.repository.TeamCachedRepository;
import objective.taskboard.repository.TeamFilterConfigurationCachedRepository;
import objective.taskboard.repository.UserTeamCachedRepository;

@Component
public class RefreshCacheTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RefreshCacheTask.class);
    @Autowired
    private UserTeamCachedRepository userTeamRepository;

    @Autowired
    private TeamCachedRepository teamRepository;

    @Autowired
    private TeamFilterConfigurationCachedRepository teamFilterConfigurationRepository;

    @Autowired
    private FilterCachedRepository filterRepository;

    @Autowired
    private LaneCachedRepository laneRepository;

    @Autowired
    private IssueTypeConfigurationCachedRepository issueTypeConfigurationCachedRepository;

    @Autowired
    private ProjectFilterConfigurationCachedRepository projectRepository;

    public void refreshUserTeam() {
        log.info("Refreshing UserTeam cache...");
        userTeamRepository.loadCache();
        List<UserTeam> users = userTeamRepository.getCache();
        log.info(format("UserTeam cache refreshed, %d records loaded.", users.size()));
    }

    public void refreshTeam() {
        log.info("Refreshing Team cache...");
        teamRepository.loadCache();
        List<Team> team = teamRepository.getCache();
        log.info(format("Team cache refreshed, %d records loaded.", team.size()));
    }

    public void refreshTeamFilterConfiguration() {
        log.info("Refreshing Team Filter Configuration cache...");
        teamFilterConfigurationRepository.loadCache();
        List<TeamFilterConfiguration> teamFilterConfiguration = teamFilterConfigurationRepository.getCache();
        log.info(format("Team Filter Configuration cache refreshed, %d records loaded.", teamFilterConfiguration.size()));
    }

    public void refreshFilter() {
        log.info("Refreshing Filter cache...");
        filterRepository.loadCache();
        List<Filter> filter = filterRepository.getCache();
        log.info(format("Filter cache refreshed, %d records loaded.", filter.size()));
    }

    public void refreshLane() {
        log.info("Refreshing Lane cache...");
        laneRepository.loadCache();
        List<Lane> lane = laneRepository.getAll();
        log.info(format("Lane cache refreshed, %d records loaded.", lane.size()));
    }

    public void refreshProject() {
        log.info("Refreshing Project cache...");
        projectRepository.loadCache();
        List<ProjectFilterConfiguration> project = projectRepository.getProjects();
        log.info(format("Project cache refreshed, %d records loaded.", project.size()));
    }

    public void refreshIssueTypeVisibility() {
        log.info("Refreshing issue type visibility cache...");
        issueTypeConfigurationCachedRepository.loadCache();
        List<IssueTypeConfiguration> configs = issueTypeConfigurationCachedRepository.getCache();
        log.info(format("Issue type visibility cache refreshed, %d records loaded.", configs.size()));
    }
}
