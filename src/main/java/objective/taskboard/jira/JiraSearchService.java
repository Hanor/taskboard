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
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractLinkedParentKey;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractRealParent;
import static org.apache.commons.lang3.StringUtils.join;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.google.common.util.concurrent.Uninterruptibles;
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

import objective.taskboard.domain.ParentIssueLink;
import objective.taskboard.domain.converter.IssueParent;
import objective.taskboard.jira.JiraService.ParametrosDePesquisaInvalidosException;
import objective.taskboard.jira.JiraService.PermissaoNegadaException;
import objective.taskboard.jira.endpoint.JiraEndpointAsMaster;
import objective.taskboard.repository.ParentIssueLinkRepository;
import org.springframework.web.client.HttpServerErrorException;

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

    private static final String PATH_REST_API_SEARCH = "/rest/api/latest/search";

    @Autowired
    private JiraProperties properties;

    @Autowired
    private JiraEndpointAsMaster jiraEndpointAsMaster;
    
    @Autowired
    private ParentIssueLinkRepository parentIssueLinkRepository;
    
    private List<String> parentIssueLinks;
    
    @PostConstruct
    private void loadParentIssueLinks() {
        parentIssueLinks = parentIssueLinkRepository.findAll().stream()
                               .map(ParentIssueLink::getDescriptionIssueLink)
                               .collect(toList());
    }

    public void searchIssuesAndParents(SearchIssueVisitor visitor, String jql) {
        Set<String> found = new LinkedHashSet<>();
        Set<String> missing = new LinkedHashSet<>();
        
        String currentJql = jql;
        do {
            if (missing.size() > 0) 
                log.debug("⬣⬣⬣⬣⬣  searchingMissingParents");
            
            missing.clear();
            searchIssues(currentJql, issue -> {
                    visitor.processIssue(issue);
                    
                    missing.remove(issue.getKey());
                    found.add(issue.getKey());
                    getParentKey(issue)      .ifPresent(key-> { if (!found.contains(key)) missing.add(key); });
                    getLinkedParentKey(issue).ifPresent(key-> { if (!found.contains(key)) missing.add(key); });
            });
            
            currentJql = "key in ("+join(missing,",")+")";
        }
        while(missing.size() > 0);
        visitor.complete();
    }
    
    public void searchIssues(String jql, SearchIssueVisitor visitor, String... additionalFields) {       
        Validate.notNull(jql);
        log.debug("⬣⬣⬣⬣⬣  searchIssues");
        

        for (int i = 0; true; i++) {
            SearchResult searchResult = searchRequest(jql, i, additionalFields);
            
            List<Issue> issuesSearchResult = newArrayList(searchResult.getIssues());

            issuesSearchResult.stream().forEach(item->visitor.processIssue(item));
            
            boolean searchComplete = issuesSearchResult.isEmpty() || issuesSearchResult.size() < searchResult.getMaxResults();
			if (searchComplete)
                break;
        }
        visitor.complete();
        log.debug("⬣⬣⬣⬣⬣  searchIssues complete");
    }

    private SearchResult searchRequest(String jql, int startFrom, String[] additionalFields) {
        SearchResultJsonParser searchResultParser = new SearchResultJsonParser();
        Set<String> fields = getFields(additionalFields);
        try {
            JSONObject searchRequest = new JSONObject();
            searchRequest.put(JQL_ATTRIBUTE, jql)
                         .put(EXPAND_ATTRIBUTE, EXPAND)
                         .put(MAX_RESULTS_ATTRIBUTE, MAX_RESULTS)
                         .put(START_AT_ATTRIBUTE, startFrom * MAX_RESULTS)
                         .put(FIELDS_ATTRIBUTE, fields);

            boolean retry = true;
            String jsonResponse = null;
            for(int attempts = 0; retry && attempts < 3; ++attempts) {
                try {
                    jsonResponse = jiraEndpointAsMaster.postWithRestTemplate(PATH_REST_API_SEARCH, APPLICATION_JSON, searchRequest);
                    retry = false;
                } catch (HttpServerErrorException ex) {
                    if (ex.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT) {
                        Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
                    } else {
                        throw ex;
                    }
                }
            }

            SearchResult searchResult = searchResultParser.parse(new JSONObject(jsonResponse));
            log.debug("⬣⬣⬣⬣⬣  searchIssues... ongoing..." + (searchResult.getStartIndex() + searchResult.getMaxResults())+ "/" + searchResult.getTotal());
            return searchResult;
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
   
    private Optional<String> getParentKey(Issue issue) {
        IssueParent parent = extractRealParent(issue);
        if (parent == null)
            return Optional.empty();
        
        return Optional.of(parent.getKey());
    }
    
    private Optional<String> getLinkedParentKey(Issue issue) {
        String linkedParentKey = extractLinkedParentKey(properties, issue, parentIssueLinks);
        if (linkedParentKey == null)
            return Optional.empty();

        return Optional.of(linkedParentKey);
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
            properties.getCustomfield().getClassOfService().getId(),
            properties.getCustomfield().getCoAssignees().getId(),
            properties.getCustomfield().getBlocked().getId(),
            properties.getCustomfield().getLastBlockReason().getId(),
            properties.getCustomfield().getAdditionalEstimatedHours().getId(),
            properties.getCustomfield().getRelease().getId());
        fields.addAll(properties.getCustomfield().getTShirtSize().getIds());
        fields.addAll(asList(additionalFields));
        return fields;
    }
}
