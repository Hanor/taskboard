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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.issueBuffer.CardRepo;
import objective.taskboard.jira.client.JiraIssueDto;

@Service
public class JiraIssueService {

    @Autowired
    private JiraSearchService jiraSearchService;
        
    @Autowired
    private JiraIssueJqlBuilderService jqlService;

    public Optional<JiraIssueDto> searchIssueByKey(final String key) {
        final AtomicReference<JiraIssueDto> foundIssue = new AtomicReference<JiraIssueDto>();
        searchIssues(issue -> foundIssue.set(issue), "key IN (" + key + ")", "subtasks");
        
        return Optional.ofNullable(foundIssue.get());
    }

    public void searchAllProjectIssues(SearchIssueVisitor visitor, CardRepo cardsRepo) {
        jiraSearchService.searchIssues(jqlService.projectsJql(cardsRepo), visitor);
    }

    private void searchIssues(SearchIssueVisitor visitor, String additionalJqlCondition, String... additionalFields) {
        String jql = jqlService.projectsSqlWithoutTimeConstraint();
        if (additionalJqlCondition != null) 
            jql = "(" + additionalJqlCondition + ") AND " + jql;
        
        jiraSearchService.searchIssues(jql, visitor, additionalFields);
    }
}
