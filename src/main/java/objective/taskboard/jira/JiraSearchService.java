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

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.atlassian.jira.rest.client.api.RestClientException;

import objective.taskboard.jira.JiraService.ParametrosDePesquisaInvalidosException;
import objective.taskboard.jira.JiraService.PermissaoNegadaException;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.client.JiraIssueDtoSearch;
import objective.taskboard.jira.endpoint.JiraEndpointAsMaster;

@Service
public class JiraSearchService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JiraSearchService.class);
    private static final String JQL_ATTRIBUTE = "jql";
    private static final String EXPAND_ATTRIBUTE = "expand";
    private static final String MAX_RESULTS_ATTRIBUTE = "maxResults";
    private static final String START_AT_ATTRIBUTE = "startAt";
    private static final String FIELDS_ATTRIBUTE = "fields";

    private static final Set<String> EXPAND = newHashSet("schema", "names", "changelog");
    private static final int MAX_RESULTS = 100;

    @Autowired
    private JiraProperties properties;

    @Autowired
    private JiraEndpointAsMaster jiraEndpointAsMaster;
    
    public void searchIssues(String jql, SearchIssueVisitor visitor, String... additionalFields) {       
        Validate.notNull(jql);
        log.debug("⬣⬣⬣⬣⬣  searchIssues");
        

        for (int i = 0; true; i++) {
            JiraIssueDtoSearch searchResult = searchRequest(jql, i, additionalFields);
            
            List<JiraIssueDto> issuesSearchResult = searchResult.getIssues();

            issuesSearchResult.stream().forEach(item->visitor.processIssue(item));
            
            boolean searchComplete = issuesSearchResult.isEmpty() || issuesSearchResult.size() < searchResult.getMaxResults();
			if (searchComplete)
                break;
        }
        visitor.complete();
        log.debug("⬣⬣⬣⬣⬣  searchIssues complete");
    }

    private JiraIssueDtoSearch searchRequest(String jql, int startFrom, String[] additionalFields) {
        Set<String> fields = getFields(additionalFields);
        try {
            Map<String, Object> input = new LinkedHashMap<>();
            input.put(JQL_ATTRIBUTE, jql);
            input.put(EXPAND_ATTRIBUTE, EXPAND);
            input.put(MAX_RESULTS_ATTRIBUTE, MAX_RESULTS);
            input.put(START_AT_ATTRIBUTE, startFrom * MAX_RESULTS);
            input.put(FIELDS_ATTRIBUTE, fields);
            
            JiraIssueDtoSearch searchResult = 
                    jiraEndpointAsMaster
                            .request(JiraIssueDtoSearch.Service.class)
                            .search(input);
            
            log.debug("⬣⬣⬣⬣⬣  searchIssues... ongoing..." + (searchResult.getStartAt() + searchResult.getMaxResults())+ "/" + searchResult.getTotal());
            return searchResult;
        }
        catch (RestClientException|HttpClientErrorException e) {
            long statusCode = extractStatusCode(e);
            
            log.error("Request failed: " + jql);
            if (HttpStatus.SERVICE_UNAVAILABLE.value() == statusCode)
                throw new JiraServiceUnavailable(e);
            
            if (HttpStatus.FORBIDDEN.value() == statusCode)
                throw new PermissaoNegadaException(e);
            
            if (HttpStatus.BAD_REQUEST.value() == statusCode)
                throw new ParametrosDePesquisaInvalidosException(e);
            throw new IllegalStateException(e);
        }
    }

    private long extractStatusCode(Exception e) {
        if (e instanceof RestClientException)
            return ((RestClientException) e).getStatusCode().or(0);
        return ((HttpClientErrorException)e).getRawStatusCode();
    }

    private Set<String> getFields(String[] additionalFields) {
        Set<String> fields = newHashSet(
            "parent", "project", "status", "created", "updated", "issuelinks",
            "issuetype", "summary", "description", "name", "assignee", "reporter", 
            "priority", "labels", "components", "timetracking",
            "worklog",
            properties.getCustomfield().getClassOfService().getId(),
            properties.getCustomfield().getCoAssignees().getId(),
            properties.getCustomfield().getBlocked().getId(),
            properties.getCustomfield().getLastBlockReason().getId(),
            properties.getCustomfield().getAdditionalEstimatedHours().getId(),
            properties.getCustomfield().getRelease().getId());
        fields.addAll(properties.getCustomfield().getTShirtSize().getIds());
        fields.addAll(properties.getSubtaskCreatorRequiredFieldsIds());
        fields.addAll(asList(additionalFields));
        return fields;
    }

}
