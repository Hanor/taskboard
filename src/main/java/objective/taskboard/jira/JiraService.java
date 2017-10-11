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
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import objective.taskboard.jira.data.JiraIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Resolution;
import com.atlassian.jira.rest.client.api.domain.ServerInfo;
import com.atlassian.jira.rest.client.api.domain.User;
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.google.common.collect.ImmutableList;

import objective.taskboard.auth.CredentialsHolder;
import objective.taskboard.jira.data.Transition;
import objective.taskboard.jira.data.Transitions;
import objective.taskboard.jira.data.Transitions.DoTransitionRequestBody;
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
    private static String MSG_UNAUTHORIZED = "Incorrect user or password";
    private static String MSG_FORBIDDEN = "The jira account is requesting a captcha challenge. Try logging in jira";

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
            if (!jse.getStatusCode().isPresent())
                throw new IllegalStateException("Jira return an unrecognized error during authentication.");

            HttpStatus httpStatus = jse.getStatusCode().get();
            log.error("Authentication error " + httpStatus.value() + " for user " + username);

            if (httpStatus == HttpStatus.UNAUTHORIZED)
                throw new AccessDeniedException(MSG_UNAUTHORIZED, ex);

            if (httpStatus == HttpStatus.FORBIDDEN)
                throw new AccessDeniedException(MSG_FORBIDDEN, ex);

        }
    }

    public void doTransition(String issueKey, Long transitionId, String resolutionName) {
        log.debug("⬣⬣⬣⬣⬣  doTransition");
        try {
            DoTransitionRequestBody requestBody = new DoTransitionRequestBody(transitionId, resolutionName);
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
            DoTransitionRequestBody requestBody = new DoTransitionRequestBody(transitionId, null);
            jiraEndpointAsMaster.request(Transitions.Service.class).doTransition(issueKey, requestBody);
        } catch (RetrofitError e) {
            throw new FrontEndMessageException(e);
        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public boolean assignToMe(objective.taskboard.data.Issue issue) {
        return new AssignIssueToUserAction(issue, CredentialsHolder.username()).call();
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

    public Issue getIssueByKey(String key) {
        log.debug("⬣⬣⬣⬣⬣  getIssueByKey");
        return jiraEndpointAsUser.executeRequest(client -> client.getIssueClient().getIssue(key));
    }

    public Issue getIssueByKeyAsMaster(String key) {
        log.debug("⬣⬣⬣⬣⬣  getIssueByKeyAsMaster");
        return jiraEndpointAsMaster.executeRequest(client -> client.getIssueClient().getIssue(key));
    }

    public String createIssue(IssueInput issueInput) {
        log.debug("⬣⬣⬣⬣⬣  createIssue");
        BasicIssue issue = jiraEndpointAsUser.executeRequest(client -> client.getIssueClient().createIssue(issueInput));
        return issue.getKey();
    }

    public String createIssueAsMaster(IssueInput issueInput) {
        log.debug("⬣⬣⬣⬣⬣  createIssue (master)");
        BasicIssue issue = jiraEndpointAsMaster.executeRequest(client -> client.getIssueClient().createIssue(issueInput));
        return issue.getKey();
    }

    public User getLoggedUser() {
        log.debug("⬣⬣⬣⬣⬣  getLoggedUser");
        return jiraEndpointAsUser.executeRequest(client -> client.getUserClient().getUser(CredentialsHolder.username()));
    }

    public void block(String issueKey, String lastBlockReason) {
        setBlocked(issueKey, true, lastBlockReason);
    }

    public void unblock(String issueKey) {
        setBlocked(issueKey, false, "");
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
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new RuntimeException("Could not update issue.", ex);
        }
    }

    public objective.taskboard.data.User getUser() {
        try {
            com.atlassian.jira.rest.client.api.domain.User jiraUser = getLoggedUser();
            return objective.taskboard.data.User.from(jiraUser.getDisplayName(), jiraUser.getName(), jiraUser.getEmailAddress(), jiraUser.getAvatarUri());
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            return null;
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

        private final objective.taskboard.data.Issue issue;
        private final String assignee;

        public AssignIssueToUserAction(objective.taskboard.data.Issue issue, String username) {
            this.issue = issue;
            assignee = username;
        }

        @Override
        public Boolean call() {
            if (alreadyAssignedToUser())
                return false;

            JiraIssue.Update request = buildRequest();
            send(request);

            return true;
        }

        private boolean alreadyAssignedToUser() {
            return assignee.equals(issue.getAssignee());
        }

        private JiraIssue.Update buildRequest() {
            // add last assignee as co-assignee
            final Set<String> coAssignees = issue.getCoAssignees()
                    .stream()
                    .map(co -> co.getName())
                    .collect(Collectors.toSet());
            coAssignees.add(issue.getAssignee());
            coAssignees.remove(assignee);

            // build request
            String assigneeField = "assignee";
            String coAssigneeField = properties.getCustomfield().getCoAssignees().getId();
            return JiraIssue.Update.builder()
                    .field(assigneeField).byName(assignee)
                    .field(coAssigneeField).byNames(coAssignees)
                    .build();
        }

        private void send(JiraIssue.Update request) {
            JiraIssue.Service issueService = jiraEndpointAsUser.request(JiraIssue.Service.class);
            try {
                Response result = issueService.update(issue.getIssueKey(), request);
                if (HttpStatus.valueOf(result.getStatus()) != HttpStatus.NO_CONTENT)
                    throw new FrontEndMessageException("Unexpected return code during AssignIssueToUserAction: " + result.getStatus());
            } catch (RetrofitError ex) {
                throw new FrontEndMessageException(ex);
            }
        }
    }
}