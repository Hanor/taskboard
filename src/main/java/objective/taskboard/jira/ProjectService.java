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

package objective.taskboard.jira;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.atlassian.jira.rest.client.api.domain.CimProject;

import objective.taskboard.auth.Authorizer;
import objective.taskboard.domain.Project;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.jira.data.Version;
import objective.taskboard.project.ProjectBaselineProvider;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;

@Service
public class ProjectService {

    @Autowired
    private ProjectFilterConfigurationCachedRepository projectRepository;

    @Autowired
    private JiraProjectService jiraProjectService;

    @Autowired
    private Authorizer authorizer;
    
    @Autowired
    private ProjectBaselineProvider baselineProvider;

    public List<Project> getNonArchivedJiraProjectsForUser() {
        return jiraProjectService.getUserProjects().values().stream()
                .filter(p -> !isArchived(p.getKey()))
                .sorted(comparing(Project::getName))
                .collect(toList());
    }

    public List<ProjectFilterConfiguration> getTaskboardProjects() {
        return projectRepository.getProjects().stream()
                .sorted((p1, p2) -> p1.getProjectKey().compareTo(p2.getProjectKey()))
                .collect(toList());
    }

    public List<ProjectFilterConfiguration> getTaskboardProjects(String... permissions) {
        List<String> allowedProjectsKeys = authorizer.getAllowedProjectsForPermissions(permissions);

        return getTaskboardProjects().stream()
                .filter(projectFilterConfiguration -> allowedProjectsKeys.contains(projectFilterConfiguration.getProjectKey()))
                .collect(toList());
    }

    public List<ProjectFilterConfiguration> getTaskboardProjects(Predicate<String> filterProjectByKey) {
        return getTaskboardProjects().stream()
                .filter(projectFilterConfiguration -> filterProjectByKey.test(projectFilterConfiguration.getProjectKey()))
                .collect(toList());
    }

    public List<ProjectFilterConfiguration> getTaskboardProjects(Predicate<String> filterProjectByKey, String... permissions) {
        return getTaskboardProjects(permissions).stream()
                .filter(projectFilterConfiguration -> filterProjectByKey.test(projectFilterConfiguration.getProjectKey()))
                .collect(toList());
    }

    public Optional<CimProject> getProjectMetadata(String projectKey) {
        if (!isNonArchivedAndUserHasAccess(projectKey))
            return Optional.empty();

        Iterable<CimProject> projects = jiraProjectService.getCreateIssueMetadata(projectKey);

        return projects.iterator().hasNext() ?
                Optional.of(projects.iterator().next()) :
                Optional.empty();
    }

    public boolean isNonArchivedAndUserHasAccess(String projectKey) {
        return !isArchived(projectKey) && jiraProjectService.getUserProjects().containsKey(projectKey);
    }

    public boolean jiraProjectExistsAndUserHasAccess(String projectKey) {
        return jiraProjectService.getUserProjects().containsKey(projectKey);
    }

    private boolean isArchived(String projectKey) {
        Optional<ProjectFilterConfiguration> projectOpt = projectRepository.getProjectByKey(projectKey);
        if (!projectOpt.isPresent())
            throw new IllegalArgumentException(projectKey + " doesn't exist");

        return projectOpt.get().isArchived();
    }

    public boolean taskboardProjectExists(String projectKey) {
        return projectRepository.exists(projectKey);
    }

    public Optional<ProjectFilterConfiguration> getTaskboardProject(String projectKey) {
        return projectRepository.getProjectByKey(projectKey);
    }
    
    public ProjectFilterConfiguration getTaskboardProjectOrCry(String projectKey) {
        return projectRepository.getProjectByKeyOrCry(projectKey);
    }

    public Optional<ProjectFilterConfiguration> getTaskboardProject(String projectKey, String... permissions) {
        List<String> allowedProjectsKeys = authorizer.getAllowedProjectsForPermissions(permissions);
        return allowedProjectsKeys.contains(projectKey) ? getTaskboardProject(projectKey) : Optional.empty();
    }

    public void saveTaskboardProject(ProjectFilterConfiguration project) {
        projectRepository.save(project);
    }

    public Version getVersion(String versionId) {
        if(versionId == null)
            return null;

        return jiraProjectService.getAllProjects().stream()
                .filter(project -> project.versions != null)
                .flatMap(project -> project.versions.stream())
                .filter(version -> versionId.equals(version.id))
                .findFirst()
                .orElse(null);
    }

    public List<LocalDate> getAvailableBaselineDates(String projectKey) {
        return baselineProvider.getAvailableDates(projectKey);
    }
}
