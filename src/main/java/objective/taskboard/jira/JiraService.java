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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Resolution;
import com.atlassian.jira.rest.client.api.domain.ServerInfo;
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.google.common.collect.ImmutableList;

import objective.taskboard.auth.CredentialsHolder;
import objective.taskboard.config.CacheConfiguration;
import objective.taskboard.data.User;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.data.JiraIssue;
import objective.taskboard.jira.data.JiraUser;
import objective.taskboard.jira.data.JiraUser.JiraUserAutocomple;
import objective.taskboard.jira.data.Transition;
import objective.taskboard.jira.data.Transitions;
import objective.taskboard.jira.data.Transitions.DoTransitionRequestBody;
import objective.taskboard.jira.data.plugin.UserDetail;
import objective.taskboard.jira.endpoint.JiraEndpoint;
import objective.taskboard.jira.endpoint.JiraEndpoint.Request;
import objective.taskboard.jira.endpoint.JiraEndpointAsLoggedInUser;
import objective.taskboard.jira.endpoint.JiraEndpointAsMaster;
import retrofit.RetrofitError;
import retrofit.client.Response;

@Service
@EnableConfigurationProperties(JiraProperties.class)
public class JiraService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JiraService.class);
    private static final String MSG_UNAUTHORIZED = "Incorrect user or password";
    private static final String MSG_FORBIDDEN = "The jira account is requesting a captcha challenge. Try logging in jira";

    @Autowired
    private JiraProperties properties;

    @Autowired
    private JiraEndpoint jiraEndpoint;

    @Autowired
    private JiraEndpointAsLoggedInUser jiraEndpointAsUser;

    @Autowired
    private JiraEndpointAsMaster jiraEndpointAsMaster;

    public void authenticate(String username, String password) {
        log.debug("⬣⬣⬣⬣⬣  authenticate");
        try {
            Request<ServerInfo> request = client -> client.getMetadataClient().getServerInfo();
            if (StringUtils.isEmpty(password))
                throw new AccessDeniedException("The password can't be empty");
            ServerInfo info = jiraEndpoint.executeRequest(username, password, request);
            if (info == null)
                throw new RuntimeException("The server did not respond");
        } catch (RuntimeException ex) {
            checkAuthenticationError(ex, username);
            log.error("Authentication error for user " + username);
            throw ex;
        }
    }

    private void checkAuthenticationError(RuntimeException ex, String username) {
        if (ex instanceof JiraServiceException) {
            JiraServiceException jse = (JiraServiceException) ex;
            Optional<HttpStatus> statusCode = jse.getStatusCode();
            if (!statusCode.isPresent())
                throw new IllegalStateException("Jira return an unrecognized error during authentication.");

            HttpStatus httpStatus = statusCode.get();
            log.error("Authentication error " + httpStatus.value() + " for user " + username);

            if (httpStatus == HttpStatus.UNAUTHORIZED)
                throw new AccessDeniedException(MSG_UNAUTHORIZED, ex);

            if (httpStatus == HttpStatus.FORBIDDEN)
                throw new AccessDeniedException(MSG_FORBIDDEN, ex);

        }
    }

    public void doTransition(String issueKey, Long transitionId, Map<String, Object> fields) {
        log.debug("⬣⬣⬣⬣⬣  doTransition");
        try {
            DoTransitionRequestBody requestBody = new DoTransitionRequestBody(transitionId, fields);
            jiraEndpointAsUser.request(Transitions.Service.class).doTransition(issueKey, requestBody);
        } catch (RetrofitError e) {
            throw new FrontEndMessageException(e);
        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void doTransitionAsMaster(String issueKey, Long transitionId) {
        log.debug("⬣⬣⬣⬣⬣  doTransition (master)");
        try {
            DoTransitionRequestBody requestBody = new DoTransitionRequestBody(transitionId);
            jiraEndpointAsMaster.request(Transitions.Service.class).doTransition(issueKey, requestBody);
        } catch (RetrofitError e) {
            throw new FrontEndMessageException(e);
        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Will find a user in jira, by name/midlename/surname/userid
     * @param namePart
     * @return
     */
    public List<JiraUserAutocomple> findUsers(String namePart) {
        log.debug("⬣⬣⬣⬣⬣  getUsersThatNameStartsWith");
        Iterable<JiraUserAutocomple> response = jiraEndpointAsUser.request(JiraUser.Service.class).findUsers(namePart);
        return ImmutableList.copyOf(response);
    }

    public boolean assignToUsers(String issueKey, List<User> assigneeList) {
        return new AssignIssueToUserAction(issueKey, assigneeList).call();
    }
    
    public String getResolutions(String transitionName) {
        log.debug("⬣⬣⬣⬣⬣  getResolutions");
        Resolution resolutionTransition = null;

        Iterable<Resolution> response = jiraEndpointAsUser.executeRequest(client -> client.getMetadataClient().getResolutions());
        List<Resolution> resolutions = newArrayList(response);

        if (properties.getTransitionsDoneNames().contains(transitionName)) {
            String done = properties.getResolutions().getDone().getName();
            resolutionTransition = resolutions.stream().filter(resolution -> resolution.getName().equals(done)).findFirst().orElse(null);
            return resolutionTransition.getName();
        } else if(properties.getTransitionsCancelNames().contains(transitionName))
            return properties.getResolutions().getCanceled().getName();
        return null;
    }

    public List<Transition> getTransitions(String issueKey) {
        log.debug("⬣⬣⬣⬣⬣  getTransitions");
        Iterable<Transition> response = jiraEndpointAsUser.request(Transitions.Service.class).get(issueKey).transitions;
        return ImmutableList.copyOf(response);
    }

    public List<Transition> getTransitionsAsMaster(String issueKey) {
        log.debug("⬣⬣⬣⬣⬣  getTransitions (master)");
        Iterable<Transition> response = jiraEndpointAsMaster.request(Transitions.Service.class).get(issueKey).transitions;
        return ImmutableList.copyOf(response);
    }

    public Optional<JiraIssueDto> getIssueByKey(String key) {
        log.debug("⬣⬣⬣⬣⬣  getIssueByKey");
        try {
            Optional<JiraIssueDto> optional = Optional.of(jiraEndpointAsUser.request(JiraIssueDto.Service.class).get(key));
            return optional;
        }catch(retrofit.RetrofitError e) {
            if (e.getResponse().getStatus() == 404)
                return Optional.empty();
            throw e;
        }
    }

    public JiraIssueDto getIssueByKeyAsMaster(String key) {
        log.debug("⬣⬣⬣⬣⬣  getIssueByKeyAsMaster");
        return jiraEndpointAsMaster.request(JiraIssueDto.Service.class).get(key);
    }

    public String createIssueAsMaster(IssueInput issueInput) {
        log.debug("⬣⬣⬣⬣⬣  createIssue (master)");
        BasicIssue issue = jiraEndpointAsMaster.executeRequest(client -> client.getIssueClient().createIssue(issueInput));
        return issue.getKey();
    }

    @Cacheable(CacheConfiguration.JIRA_USER)
    public JiraUser getJiraUser(String username) {
        log.debug("⬣⬣⬣⬣⬣  getJiraUser");
        return jiraEndpointAsUser.request(JiraUser.Service.class).get(username);
    }

    public void block(String issueKey, String lastBlockReason) {
        setBlocked(issueKey, true, lastBlockReason);
    }

    public void unblock(String issueKey) {
        setBlocked(issueKey, false, "");
    }

    public void setTeams(String issueKey, List<Long> teamsIds) {
        log.debug("⬣⬣⬣⬣⬣  setTeams");
        String assignedTeamCfId = properties.getCustomfield().getAssignedTeams().getId();
        
        new SetFieldAction(issueKey, assignedTeamCfId, StringUtils.join(teamsIds,",")).call();
    }

    private void setBlocked(String issueKey, boolean blocked, String lastBlockReason) {
        log.debug("⬣⬣⬣⬣⬣  setBlocked");
        String yesOptionId = properties.getCustomfield().getBlocked().getYesOptionId().toString();
        ComplexIssueInputFieldValue value = new ComplexIssueInputFieldValue(Collections.singletonMap("id", blocked ? yesOptionId : null));
        updateIssue(issueKey, new IssueInputBuilder()
                                    .setFieldValue(properties.getCustomfield().getBlocked().getId(), Collections.singletonList(value))
                                    .setFieldValue(properties.getCustomfield().getLastBlockReason().getId(), lastBlockReason));
    }

    private void updateIssue(String issueKey, IssueInputBuilder changes) {
        try {
            jiraEndpointAsUser.executeRequest(client -> client.getIssueClient().updateIssue(issueKey, changes.build()));
        }
        catch (FrontEndMessageException ex) {
            throw ex;
        }
        catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new RuntimeException("Could not update issue.", ex);
        }
    }

    public User getLoggedUser() {
        JiraUser loggedJiraUser = getJiraUser(CredentialsHolder.username());
        return getUser(loggedJiraUser);
    }

    protected User getUser(JiraUser jiraUser) {
        try {
            return new User(jiraUser.displayName, jiraUser.name, jiraUser.emailAddress);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            return null;
        }
    }

    public List<UserDetail.Role> getUserRoles(String userName) {
        UserDetail.Service service = jiraEndpointAsMaster.request(UserDetail.Service.class);
        try {
            return service.get(userName).roles;
        } catch (RetrofitError ex) {
            if(ex.getResponse().getStatus() == 404) {
                throw new RuntimeException("Unknown user", ex);
            } else {
                throw ex;
            }
        }
    }

    @SuppressWarnings("serial")
    public static class PermissaoNegadaException extends RuntimeException {
        public PermissaoNegadaException(Exception e) {
            super(e);
        }
    }

    @SuppressWarnings("serial")
    public static class ParametrosDePesquisaInvalidosException extends RuntimeException {
        public ParametrosDePesquisaInvalidosException(Exception e) {
            super(e);
        }
    }

    /**
     * Tries to assign issue to user. Moves last assignee to co-assignees if already has assignee.
     * Throws {@link FrontEndMessageException} if action failed
     */
    private class AssignIssueToUserAction implements Callable<Boolean> {

        private final String issueKey;
        private Queue<User> assignees;

        public AssignIssueToUserAction(String issueKey, List<User> assignees) {
            this.issueKey = issueKey;
            this.assignees = new LinkedList<User>(assignees);
        }

        @Override
        public Boolean call() {
            JiraIssue.Input request = buildRequest();
            send(request);

            return true;
        }

        private JiraIssue.Input buildRequest() {
            // build request
            String assigneeField = "assignee";
            Optional<User> firstAssignee = Optional.ofNullable(assignees.poll());
            String assignee = firstAssignee.map(u->u.name).orElse(null);
            List<String> coAssigneesNames = assignees.stream().map(a -> a.name).collect(Collectors.toList());

            // build request
            String coAssigneeField = properties.getCustomfield().getCoAssignees().getId();
            
            return JiraIssue.Input.builder()
                    .field(assigneeField).byName(assignee)
                    .field(coAssigneeField).byNames(coAssigneesNames)
                    .build();
        }

        private void send(JiraIssue.Input request) {
            JiraIssue.Service issueService = jiraEndpointAsUser.request(JiraIssue.Service.class);
            try {
                Response result = issueService.update(issueKey, request);
                if (HttpStatus.valueOf(result.getStatus()) != HttpStatus.NO_CONTENT)
                    throw new FrontEndMessageException("Unexpected return code during AssignIssueToUserAction: " + result.getStatus());
            } catch (RetrofitError ex) {
                throw new FrontEndMessageException(ex);
            }
        }
    }
    
    
    /**
     * Tries to set a given field value.
     * Throws {@link FrontEndMessageException} if action failed
     */
    private class SetFieldAction implements Callable<Boolean> {

        private final String issueKey;
        private String field;
        private String value;

        public SetFieldAction(String issueKey, String field, String value) {
            this.issueKey = issueKey;
            this.field = field;
            this.value = value;
        }

        @Override
        public Boolean call() {
            JiraIssue.Input request = buildRequest();
            send(request);
            return true;
        }

        private JiraIssue.Input buildRequest() {
            return JiraIssue.Input.builder()
                    .field(field).value(value)
                    .build();
        }

        private void send(JiraIssue.Input request) {
            JiraIssue.Service issueService = jiraEndpointAsUser.request(JiraIssue.Service.class);
            try {
                Response result = issueService.update(issueKey, request);
                if (HttpStatus.valueOf(result.getStatus()) != HttpStatus.NO_CONTENT)
                    throw new FrontEndMessageException("Unexpected return code during SetFieldAction: " + result.getStatus());
            } catch (RetrofitError ex) {
                if (HttpStatus.valueOf(ex.getResponse().getStatus()) == HttpStatus.NOT_FOUND)
                    throw new FrontEndMessageException("Issue "+issueKey+" can't be update because it wasn't found in Jira");
                throw new FrontEndMessageException(ex);
            }
        }
    }
}