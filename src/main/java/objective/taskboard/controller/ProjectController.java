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

package objective.taskboard.controller;

import static java.util.stream.Collectors.toList;
import static objective.taskboard.repository.PermissionRepository.DASHBOARD_OPERATIONAL;
import static objective.taskboard.repository.PermissionRepository.DASHBOARD_TACTICAL;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import objective.taskboard.auth.Authorizer;
import objective.taskboard.controller.ProjectCreationData.ProjectCreationDataTeam;
import objective.taskboard.data.Team;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.domain.ProjectTeam;
import objective.taskboard.domain.TeamFilterConfiguration;
import objective.taskboard.followup.FollowUpFacade;
import objective.taskboard.jira.ProjectService;
import objective.taskboard.repository.ProjectTeamRepository;
import objective.taskboard.repository.TeamCachedRepository;
import objective.taskboard.repository.TeamFilterConfigurationCachedRepository;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    ProjectTeamRepository projectTeamRepo;

    @Autowired
    TeamCachedRepository teamRepository;

    @Autowired
    TeamFilterConfigurationCachedRepository teamFilterConfigurationRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private FollowUpFacade followUpFacade;

    @Autowired
    private Authorizer authorizer;

    @RequestMapping
    public List<ProjectData> getProjectsVisibleOnTaskboard() {
        return projectService.getTaskboardProjects(projectService::isNonArchivedAndUserHasAccess).stream()
                .map(pfc -> generateProjectData(pfc))
                .collect(toList());
    }

    @RequestMapping("/dashboard")
    public List<ProjectData> getProjectsVisibleOnDashboard() {
        return projectService.getTaskboardProjects(projectService::isNonArchivedAndUserHasAccess, DASHBOARD_TACTICAL, DASHBOARD_OPERATIONAL).stream()
                .map(pfc -> generateProjectData(pfc))
                .collect(toList());
    }

    @RequestMapping(method = RequestMethod.PATCH, consumes="application/json")
    public ResponseEntity<String> updateProjectsTeams(@RequestBody ProjectData [] projectsTeams) {
        List<ProjectTeam> projectTeamCfgsToAdd = new LinkedList<>();
        List<String> errors = new LinkedList<>();
        List<ProjectTeam> projectTeamCfgsToRemove = new LinkedList<>();
        for (ProjectData ptd : projectsTeams) {
            List<ProjectTeam> projectTeamsThatAreNotInTheRequest = projectTeamRepo.findByIdProjectKey(ptd.projectKey);
            
            for (String teamName : ptd.teams) {
                Team team = teamRepository.findByName(teamName);

                if (team == null) {
                    errors.add("Team " + teamName + " not found.");
                    continue;
                }
                Optional<ProjectTeam> projectXteam = projectTeamsThatAreNotInTheRequest.stream()
                        .filter(pt -> pt.getTeamId().equals(team.getId()))
                        .findFirst();
                
                if (projectXteam.isPresent()) 
                    projectTeamsThatAreNotInTheRequest.remove(projectXteam.get());
                else {
                    ProjectTeam projectTeam = new ProjectTeam();
                    projectTeam.setProjectKey(ptd.projectKey);
                    projectTeam.setTeamId(team.getId());
                    projectTeamCfgsToAdd.add(projectTeam);
                }
            }
            projectTeamCfgsToRemove.addAll(projectTeamsThatAreNotInTheRequest);
        }
        if (!errors.isEmpty()) 
            return ResponseEntity.badRequest().body(StringUtils.join(errors,"\n"));
        
        for (ProjectTeam pt : projectTeamCfgsToAdd) 
            projectTeamRepo.save(pt);
        
        projectTeamCfgsToRemove.stream().forEach(ptcfg -> projectTeamRepo.delete(ptcfg));
        
        return ResponseEntity.ok("");
    }

    @RequestMapping(value="{projectKey}", method = RequestMethod.GET)
    public ResponseEntity<Void> get(@PathVariable("projectKey") String projectKey) {
        return projectService.jiraProjectExistsAndUserHasAccess(projectKey) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @RequestMapping(method = RequestMethod.POST, consumes="application/json")
    public void create(@RequestBody ProjectCreationData data) {
        if (projectService.taskboardProjectExists(data.projectKey))
            return;

        Boolean hasTeamsOnData = data.teams != null && !data.teams.isEmpty();
        if (hasTeamsOnData)
            validateTeamsAndWipConfigurations(data.teams);

        projectService.saveTaskboardProject(new ProjectFilterConfiguration(data.projectKey));
        if (!hasTeamsOnData)
            return;

        data.teams.forEach(team -> {
            createTeamAndConfigurations(data.projectKey, team.name, data.teamLeader, data.teamLeader, team.members);
        });
    }

    private void validateTeamsAndWipConfigurations(List<ProjectCreationDataTeam> pcdTeams) {
        for (ProjectCreationDataTeam pcdTeam : pcdTeams) {
            validateTeamDontExist(pcdTeam.name);
        }
    }

    private void validateTeamDontExist(String teamName) {
        if (teamRepository.exists(teamName))
            throw new IllegalArgumentException("Team '" + teamName + "' already exists.");
    }

    private void createTeamAndConfigurations(String projectKey, String teamName, String manager, String coach, List<String> members) {
        Team team = createTeam(teamName, manager, coach, members);
        TeamFilterConfiguration teamFilterConfiguration = createTeamFilterConfiguration(team);
        createProjectTeam(projectKey, teamFilterConfiguration);
    }

    private Team createTeam(String name, String manager, String coach, List<String> members) {
        final Team team = new Team(name, manager, coach, members);
        return teamRepository.save(team);
    }

    private TeamFilterConfiguration createTeamFilterConfiguration(Team team) {
        final TeamFilterConfiguration teamFilterConfiguration = new TeamFilterConfiguration();
        teamFilterConfiguration.setTeamId(team.getId());
        return teamFilterConfigurationRepository.save(teamFilterConfiguration);
    }

    private ProjectTeam createProjectTeam(String projectKey, TeamFilterConfiguration teamFilterConfiguration) {
        final ProjectTeam projectTeam = new ProjectTeam();
        projectTeam.setProjectKey(projectKey);
        projectTeam.setTeamId(teamFilterConfiguration.getTeamId());
        return projectTeamRepo.save(projectTeam);
    }

    private ProjectData generateProjectData(ProjectFilterConfiguration projectFilterConfiguration) {
        ProjectData projectData = new ProjectData();
        projectData.projectKey = projectFilterConfiguration.getProjectKey();
        projectData.teams.addAll(getTeams(projectFilterConfiguration));
        projectData.followUpDataHistory = followUpFacade.getHistoryGivenProject(projectData.projectKey);
        projectData.roles = authorizer.getRolesForProject(projectFilterConfiguration.getProjectKey());
       
        return projectData;
    }

    private List<String> getTeams(ProjectFilterConfiguration projectFilterConfiguration) {
        return projectTeamRepo.findAll().stream()
            .filter(projectTeam -> projectTeam.getProjectKey().equals(projectFilterConfiguration.getProjectKey()))
            .map(projectTeam -> getProjectTeamName(projectTeam))
            .collect(toList());
    }

    private String getProjectTeamName(ProjectTeam projectTeam) {
        return teamRepository.findById(projectTeam.getTeamId()).getName();
    }

}
