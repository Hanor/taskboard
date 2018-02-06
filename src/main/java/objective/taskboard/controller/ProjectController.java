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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import objective.taskboard.controller.ProjectCreationData.ProjectCreationDataTeam;
import objective.taskboard.data.Team;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.domain.ProjectTeam;
import objective.taskboard.domain.TeamFilterConfiguration;
import objective.taskboard.followup.FollowUpFacade;
import objective.taskboard.jira.ProjectService;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.repository.ProjectTeamRepository;
import objective.taskboard.repository.TeamCachedRepository;
import objective.taskboard.repository.TeamFilterConfigurationCachedRepository;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    ProjectFilterConfigurationCachedRepository projectRepository;

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

    @RequestMapping
    public List<ProjectData> get() {
        List<ProjectTeam> projects = projectTeamRepo.findAll().stream()
                .filter(t -> projectService.isProjectVisible(t.getProjectKey()))
                .sorted((p1, p2) -> p1.getProjectKey().compareTo(p2.getProjectKey()))
                .collect(toList());

        List<ProjectData> response = new ArrayList<>();
        Map<String, ProjectData> projectXData = new LinkedHashMap<>();
        for (ProjectTeam projectTeam : projects) {   
            if (!projectXData.containsKey(projectTeam.getProjectKey())) {
                ProjectData value = new ProjectData(projectTeam);
                response.add(value);
                projectXData.put(projectTeam.getProjectKey(), value);
            }
            ProjectData p = projectXData.get(projectTeam.getProjectKey());
            p.teams.add(getProjectTeamName(projectTeam));
        }
        
        List<ProjectFilterConfiguration> projectFilter = projectRepository.
                getProjects().stream()
                .filter(t -> projectService.isProjectVisible(t.getProjectKey()))
                .collect(toList());
        
        for (ProjectFilterConfiguration projectFilterConfiguration : projectFilter) {
            if (!projectXData.containsKey(projectFilterConfiguration.getProjectKey())) {
                ProjectData value = new ProjectData();
                value.projectKey = projectFilterConfiguration.getProjectKey();
                response.add(value);
                projectXData.put(value.projectKey, value);
            }
        }

        for (ProjectData projectData : response) {
            String projectKey = projectData.projectKey;
            projectData.followUpDataHistory = followUpFacade.getHistoryGivenProjects(projectKey);
        }

        return response;
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
                        .filter(pt -> pt.getTeamId() == team.getId())
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
        List<ProjectFilterConfiguration> projects = projectRepository.getProjects();
        for (ProjectFilterConfiguration projectFilterConfiguration : projects) {
            if (projectFilterConfiguration.getProjectKey().equals(projectKey))
                return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(method = RequestMethod.POST, consumes="application/json")
    public void create(@RequestBody ProjectCreationData data) {
        if (projectRepository.exists(data.projectKey))
            return;
        Boolean hasTeamsOnData = data.teams != null && !data.teams.isEmpty();
        if (hasTeamsOnData)
            validateTeamsAndWipConfigurations(data.teams);
        createProjectFilterConfiguration(data.projectKey);
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

    private void createProjectFilterConfiguration(String projectKey) {
        final ProjectFilterConfiguration configuration = new ProjectFilterConfiguration();
        configuration.setProjectKey(projectKey);
        projectRepository.save(configuration);
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

    private String getProjectTeamName(ProjectTeam projectTeam) {
        return teamRepository.findById(projectTeam.getTeamId()).getName();
    }
}
