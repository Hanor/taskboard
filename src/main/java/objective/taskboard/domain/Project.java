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
package objective.taskboard.domain;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Streams;

import objective.taskboard.jira.data.Version;

public class Project {

    private String key;
    private String name;
    private List<Long> teamsIds;
    private List<Version> versions;

    public static Project from(com.atlassian.jira.rest.client.api.domain.Project jiraProject,
            ProjectFilterConfiguration projectFilterConfiguration) {
        Project project = new Project();
        project.setKey(jiraProject.getKey());
        project.setName(jiraProject.getName());
        project.setTeamsIds(Arrays.asList(projectFilterConfiguration.getDefaultTeam()));

        List<Version> versions = newArrayList();
        if (jiraProject.getVersions() != null)
            versions = Streams.stream(jiraProject.getVersions())
                            .map(v -> new Version(Long.toString(v.getId()), v.getName()))
                            .collect(toList());

        project.setVersions(versions);
        return project;
    }

    public String getKey() {
        return this.key;
    }

    public String getName() {
        return this.name;
    }

    public List<Long> getTeamsIds() {
        return this.teamsIds;
    }

    public List<Version> getVersions() {
        return this.versions;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setTeamsIds(final List<Long> teamsIds) {
        this.teamsIds = teamsIds;
    }

    public void setVersions(final List<Version> versions) {
        this.versions = versions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Project project = (Project) o;

        if (key != null ? !key.equals(project.key) : project.key != null) return false;
        if (name != null ? !name.equals(project.name) : project.name != null) return false;
        if (teamsIds != null ? !teamsIds.equals(project.teamsIds) : project.teamsIds != null) return false;
        return versions != null ? versions.equals(project.versions) : project.versions == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (teamsIds != null ? teamsIds.hashCode() : 0);
        result = 31 * result + (versions != null ? versions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Project{" +
                "key='" + key + '\'' +
                ", name='" + name + '\'' +
                ", teamsIds=" + teamsIds +
                ", versions=" + versions +
                '}';
    }

}
