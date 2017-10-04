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

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssuelinksType;

@Service
public class JiraIssueService {

    @Autowired
    private JiraSearchService jiraSearchService;
    
    @Autowired
    private MetadataService metadataService;

    @Autowired
    private JiraProperties properties;
    
    @Autowired
    private JiraIssueJqlBuilderService jqlService;

    public void searchIssuesByKeys(final List<String> keys, SearchIssueVisitor visitor) {
        if (keys.isEmpty()) 
            return;
        
        searchIssues(visitor, "key IN (" + String.join(",", keys) + ")", "subtasks");
    }

    public Optional<Issue> searchIssueByKey(final String key) {
        final AtomicReference<Issue> foundIssue = new AtomicReference<Issue>();
        searchIssuesByKeys(asList(key), issue -> foundIssue.set(issue));
        
        return Optional.ofNullable(foundIssue.get());
    }

    public void searchIssueSubTasksAndDemandedByKey(String key, SearchIssueVisitor visitor) {
        IssuelinksType demandLink = metadataService.getIssueLinksMetadata().get(properties.getIssuelink().getDemandId().toString());
        String jql = "parent = " + key +  " OR" + 
                " issuefunction in linkedIssuesOf('key = " + key + "', '" + demandLink.getOutward() + "')";

        jiraSearchService.searchIssues(jql, visitor);
    }

    public void searchAllWithParents(SearchIssueVisitor visitor) {
        jiraSearchService.searchIssuesAndParents(visitor,jqlService.buildQueryForIssues());
    }
    
    public void searchAllProjectIssues(SearchIssueVisitor visitor) {
        jiraSearchService.searchIssues(jqlService.projectsJql(), visitor);
    }

    private void searchIssues(SearchIssueVisitor visitor, String additionalJqlCondition, String... additionalFields) {
        String jql = jqlService.buildQueryForIssues();
        if (additionalJqlCondition != null) 
            jql = "(" + additionalJqlCondition + ") AND " + jql;
        
        jiraSearchService.searchIssues(jql, visitor, additionalFields);
    }
}
