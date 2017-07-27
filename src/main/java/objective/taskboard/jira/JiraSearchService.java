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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.json.SearchResultJsonParser;

import lombok.extern.slf4j.Slf4j;
import objective.taskboard.jira.JiraService.ParametrosDePesquisaInvalidosException;
import objective.taskboard.jira.JiraService.PermissaoNegadaException;
import objective.taskboard.jira.endpoint.JiraEndpointAsMaster;

@Slf4j
@Service
public class JiraSearchService {

    private static final String JQL_ATTRIBUTE = "jql";
    private static final String EXPAND_ATTRIBUTE = "expand";
    private static final String MAX_RESULTS_ATTRIBUTE = "maxResults";
    private static final String START_AT_ATTRIBUTE = "startAt";
    private static final String FIELDS_ATTRIBUTE = "fields";

    private static final Set<String> EXPAND = newHashSet("schema", "names", "changelog");
    private static final int MAX_RESULTS = 100;

    private static final String PATH_REST_API_SEARCH = "/rest/api/latest/search";
    
    @Autowired
    private JiraProperties properties;

    @Autowired
    private JiraEndpointAsMaster jiraEndpointAsMaster;

    static int seq = 0;
    public List<Issue> searchIssues(String jql) {
        Validate.notNull(jql);
        log.debug("⬣⬣⬣⬣⬣  searchIssues");
        try {
            SearchResultJsonParser searchResultParser = new SearchResultJsonParser();
            List<Issue> listIssues = new ArrayList<>();

            for (int i = 0; true; i++) {
                JSONObject searchRequest = new JSONObject();
                searchRequest.put(JQL_ATTRIBUTE, jql)
                             .put(EXPAND_ATTRIBUTE, EXPAND)
                             .put(MAX_RESULTS_ATTRIBUTE, MAX_RESULTS)
                             .put(START_AT_ATTRIBUTE, i * MAX_RESULTS)
                             .put(FIELDS_ATTRIBUTE, getFields());

                String jsonResponse = jiraEndpointAsMaster.postWithRestTemplate(PATH_REST_API_SEARCH, APPLICATION_JSON, searchRequest);
                SearchResult searchResult = searchResultParser.parse(new JSONObject(jsonResponse));
                
                log.debug("⬣⬣⬣⬣⬣  searchIssues... ongoing..." + (searchResult.getStartIndex() + searchResult.getMaxResults())+ "/" + searchResult.getTotal());
                
                List<Issue> issuesSearchResult = newArrayList(searchResult.getIssues());

                listIssues.addAll(issuesSearchResult);
                
                if (issuesSearchResult.isEmpty() || issuesSearchResult.size() < searchResult.getMaxResults())
                    break;

            }
            log.debug("⬣⬣⬣⬣⬣  searchIssues complete");
            return listIssues;
        } 
        catch (JSONException e) {
            log.error(jql);
            throw new IllegalStateException(e);        
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

    private Set<String> getFields() {
        Set<String> fields = newHashSet(
            "parent", "project", "status", "created", "updated", "issuelinks",
            "issuetype", "summary", "description", "name", "assignee", "reporter", 
            "priority", "labels", "components", "timetracking",
            properties.getCustomfield().getClassOfService().getId(),
            properties.getCustomfield().getCoAssignees().getId(),
            properties.getCustomfield().getBlocked().getId(),
            properties.getCustomfield().getLastBlockReason().getId(),
            properties.getCustomfield().getAdditionalEstimatedHours().getId(),
            properties.getCustomfield().getRelease().getId());
        fields.addAll(properties.getCustomfield().getTShirtSize().getIds());
        return fields;
    }
}
