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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.domain.converter.IssueMetadata.IssueCoAssignee;
import objective.taskboard.filterConfiguration.TeamFilterConfigurationService;

@Service
public class IssueTeamService {

    @Autowired
    private TeamFilterConfigurationService teamFilterConfigurationService;

    public Map<String, List<String>> getIssueTeams(IssueMetadata metadata, IssueMetadata parentMetadata) throws InvalidTeamException {
        Map<String, List<String>> usersTeam = getIssueUsersTeams(metadata);
        if (!usersTeam.isEmpty())
            return usersTeam;

        Map<String, List<String>> parentUsersTeam = getParentIssueUsersTeams(parentMetadata);
        if (!parentUsersTeam.isEmpty())
            return parentUsersTeam;

        String reporter = metadata.getReporter();
        if (reporter == null)
            return newHashMap();

        return getUsersTeams(newArrayList(reporter), metadata.getProjectKey());
    }

    private Map<String, List<String>> getIssueUsersTeams(IssueMetadata metadata) throws InvalidTeamException {
        List<String> users = newArrayList();

        String assignee = metadata.getAssignee();
        if (assignee != null)
            users.add(assignee);
        for (IssueCoAssignee coAssignee : metadata.getCoAssignees())
            users.add(coAssignee.getName());

        users = users.stream()
                .distinct()
                .collect(toList());

        return getUsersTeams(users, metadata.getProjectKey());
    }

    private Map<String, List<String>> getUsersTeams(List<String> users, String projectKey) throws InvalidTeamException {
        Map<String, List<String>> usersTeams = newHashMap();

        boolean foundSomeTeam = false;
        for (String user : users) {
            List<String> teams = teamFilterConfigurationService.getConfiguredTeamsNamesByUserAndProject(user, projectKey);

            if (!teams.isEmpty())
                foundSomeTeam = true;

            usersTeams.put(user, teams);
        }

        if (!users.isEmpty() && !foundSomeTeam)
            throw new InvalidTeamException(users);

        return usersTeams;
    }

    private Map<String, List<String>> getParentIssueUsersTeams(IssueMetadata parentMetadata) throws InvalidTeamException {
        if (parentMetadata == null)
            return newHashMap();
        return getIssueUsersTeams(parentMetadata);
    }

    public static class InvalidTeamException extends Exception {
        private static final long serialVersionUID = 1L;

        private final List<String> usersInInvalidTeam;

        public InvalidTeamException(List<String> usersInInvalidTeam) {
            this.usersInInvalidTeam = Collections.unmodifiableList(usersInInvalidTeam);
        }

        public List<String> getUsersInInvalidTeam() {
            return usersInInvalidTeam;
        }
    }

}