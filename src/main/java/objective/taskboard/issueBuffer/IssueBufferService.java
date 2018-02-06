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
package objective.taskboard.issueBuffer;

import static java.util.stream.Collectors.toList;
import static objective.taskboard.repository.PermissionRepository.ADMINISTRATIVE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.google.common.util.concurrent.Uninterruptibles;

import objective.taskboard.auth.Authorizer;
import objective.taskboard.data.Issue;
import objective.taskboard.data.ProjectsUpdateEvent;
import objective.taskboard.data.TaskboardIssue;
import objective.taskboard.database.IssuePriorityService;
import objective.taskboard.domain.converter.JiraIssueToIssueConverter;
import objective.taskboard.domain.converter.ParentProvider;
import objective.taskboard.issue.IssueUpdate;
import objective.taskboard.issue.IssueUpdateType;
import objective.taskboard.issue.IssuesUpdateEvent;
import objective.taskboard.jira.FrontEndMessageException;
import objective.taskboard.jira.JiraIssueService;
import objective.taskboard.jira.JiraService;
import objective.taskboard.jira.ProjectService;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.data.WebhookEvent;
import objective.taskboard.task.IssueEventProcessScheduler;
import objective.taskboard.task.JiraEventProcessor;

@Service
public class IssueBufferService {
    private static final String CACHE_FILENAME = "issues.dat";

    private static final Logger log = LoggerFactory.getLogger(IssueBufferService.class);

    @Autowired
    public ApplicationEventPublisher eventPublisher;

    @Autowired
    private JiraIssueToIssueConverter issueConverter;

    @Autowired
    private JiraIssueService jiraIssueService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private JiraService jiraBean;

    @Autowired
    private IssuePriorityService issuePriorityService;

    @Autowired
    private IssueEventProcessScheduler issueEvents;
    
    @Autowired
    private CardRepoService cardsRepoService;

    @Autowired
    private Authorizer authorizer;

    private CardRepo cardsRepo;
    
    private final ParentProvider parentProviderFetchesMissingParents = parentKey -> {
        Issue parent = cardsRepo.get(parentKey);
        if (parent == null)
            parent = updateIssueBufferFetchParentIfNeeded(jiraBean.getIssueByKeyAsMaster(parentKey));
        return Optional.of(parent);
    };

    private IssueBufferState state = IssueBufferState.uninitialised;

    
    private boolean isUpdatingTaskboardIssuesBuffer = false;

    private List<IssueUpdate> issuesUpdatedByEvent = new ArrayList<>();
    private Set<String> projectsUpdatedByEvent = new HashSet<>();

    @PostConstruct
    private void loadCache() {
        cardsRepo = cardsRepoService.from(CACHE_FILENAME);
        if (cardsRepo.size() > 0) {
            state = IssueBufferState.ready;
            return;
        }
        
        updateIssueBuffer();

        log.info("Card repo is not initialized. This might take some time..");
        
        while(!getState().isInitialized()) 
            Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        
        log.info("Card repo initialized");
    }
    
    private synchronized void saveCache() {
        cardsRepo.commit();
    }

    public IssueBufferState getState() {
        return state;
    }

    public synchronized void updateIssueBuffer() {
        if (isUpdatingTaskboardIssuesBuffer)
            return;
        
        isUpdatingTaskboardIssuesBuffer = true;
        Thread thread = new Thread(() -> {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            try {
                updateState(state.start());
                
                IssueBufferServiceSearchVisitor visitor = new IssueBufferServiceSearchVisitor(issueConverter, this);
                jiraIssueService.searchAllProjectIssues(visitor, cardsRepo);

                log.info("Issue buffer - processed " + visitor.getProcessedCount() + " issues");
                
                updateState(state.done());
                saveCache();
            }catch(Exception e) {
                updateState(state.error());
                log.error("objective.taskboard.issueBuffer.IssueBufferService.updateIssueBuffer - Failed to bring issues", e);
            }
            finally {
                log.debug("updateIssueBuffer time spent " +stopWatch.getTime());
                isUpdatingTaskboardIssuesBuffer = false;
            }
        });
        thread.setName("Buffer.update");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateState(IssueBufferState state) {
        this.state = state;
        if (state.isInitialized())
            eventPublisher.publishEvent(new IssueCacheUpdateEvent(this, state));
    }

    public Issue updateIssueBuffer(final String key) {
        StopWatch watch = new StopWatch();
        watch.start();
        Optional<JiraIssueDto> foundIssue =  jiraBean.getIssueByKey(key);
        if (!foundIssue.isPresent()) 
            return cardsRepo.remove(key);
        
        log.debug("Time to fetch issue: " + watch.getTime());

        return updateIssueBufferFetchParentIfNeeded(foundIssue.get());
    }

    public synchronized Issue updateIssueBufferFetchParentIfNeeded(final JiraIssueDto jiraIssue) {
        final Issue issue = issueConverter.convertSingleIssue(jiraIssue, parentProviderFetchesMissingParents);
        putIssue(issue);

        updateIssueBuffer();// triggers a background update, because this change might affect other issues 

        return getIssueByKey(issue.getIssueKey());
    }
    
    private synchronized Issue putJiraIssue(JiraIssueDto jiraIssue) {
        final Issue issue = issueConverter.convertSingleIssue(jiraIssue, parentProviderFetchesMissingParents);
        putIssue(issue);

        return getIssueByKey(issue.getIssueKey());
    }

    public synchronized void notifyProjectUpdate(final String projectKey) {
        projectsUpdatedByEvent.add(projectKey);
    }

    public synchronized void notifyIssueUpdate(final Issue issue) {
        issuesUpdatedByEvent.add(new IssueUpdate(issue, IssueUpdateType.UPDATED));
    }

    public synchronized void updateByEvent(WebhookEvent event, final String issueKey, Optional<JiraIssueDto> issue) {
        if (event == WebhookEvent.ISSUE_DELETED) {
            issuesUpdatedByEvent.add(new IssueUpdate(cardsRepo.get(issueKey), IssueUpdateType.DELETED));
            cardsRepo.remove(issueKey);
            return;
        }
        if (!issue.isPresent())
            return;

        Issue updated = putJiraIssue(issue.get());

        IssueUpdateType updateType = IssueUpdateType.UPDATED;
        if (event == WebhookEvent.ISSUE_CREATED)
            updateType = IssueUpdateType.CREATED;

        issuesUpdatedByEvent.add(new IssueUpdate(updated, updateType));

        scheduleNotificationSubtasksUpdate(updated);
    }

    public synchronized void startBatchUpdate() {
        issuesUpdatedByEvent.clear();
        projectsUpdatedByEvent.clear();
    }

    public synchronized void finishBatchUpdate() {
        saveCache();
        
        if (issuesUpdatedByEvent.size() > 0)
            eventPublisher.publishEvent(new IssuesUpdateEvent(this, issuesUpdatedByEvent));
        issuesUpdatedByEvent.clear();

        if (projectsUpdatedByEvent.size() > 0)
            eventPublisher.publishEvent(new ProjectsUpdateEvent(this, projectsUpdatedByEvent.toArray(new String[0])));
        projectsUpdatedByEvent.clear();
    }

    public synchronized List<Issue> getIssues() {
        return cardsRepo.values().stream()
                .filter(t -> projectService.isNonArchivedAndUserHasAccess(t.getProjectKey()))
                .filter(t -> t.isVisible())
                .collect(toList());
    }
    
    public synchronized List<Issue> getAllIssues() {
        return cardsRepo.values().stream().collect(toList());
    }

    public synchronized Issue getIssueByKey(String key) {
        return cardsRepo.get(key);
    }

    public synchronized Issue assignToMe(String issueKey) {
        Issue issue = getIssueByKey(issueKey);
        if(jiraBean.assignToMe(issue)) {
            return updateIssueBuffer(issueKey);
        } else {
            return issue;
        }
    }

    public synchronized Issue doTransition(String issueKey, Long transitionId, Map<String, Object> fields) {
        StopWatch watch = new StopWatch();
        watch.start();
        jiraBean.doTransition(issueKey, transitionId, fields);
        log.debug("Time to perform transition for issue key " + issueKey + " : " + watch.getTime());

        Issue issue = updateIssueBuffer(issueKey);
        updateIssueBuffer();
        return issue;
    }

    private void putIssue(Issue issue) {
        cardsRepo.putOnlyIfNewer(issue);
    }

    public synchronized List<Issue> reorder(String[] issues) {
        ReorderIssuesAction action = new ReorderIssuesAction(issues);
        return action.call();
    }
    
    private synchronized void updateIssuesPriorities(List<TaskboardIssue> issuesPriorities) {
        for (TaskboardIssue taskboardIssue : issuesPriorities) { 
            Issue issue = cardsRepo.get(taskboardIssue.getIssueKey());
            issueEvents.add(new InternalUpdateIssue(issue.getProjectKey(), issue.getIssueKey()));
        }
    }

    /**
     * Update issue in buffer and notify the taskboard
     * @param issue to update
     * @return if issue is updated in buffer and notified the taskboard successfully
     */
    public synchronized boolean updateIssue(Issue issue) {
        if (!cardsRepo.putOnlyIfNewer(issue))
            return false;
        issueEvents.add(new InternalUpdateIssue(issue.getProjectKey(), issue.getIssueKey()));
        scheduleNotificationSubtasksUpdate(issue);
        return true;
    }

    private synchronized void scheduleNotificationSubtasksUpdate(Issue parent) {
        for (Issue subtask : parent.getSubtaskCards()) {
            issueEvents.add(new InternalUpdateIssue(subtask.getProjectKey(), subtask.getIssueKey()));
            scheduleNotificationSubtasksUpdate(subtask);
        }
    }

    private class InternalUpdateIssue implements JiraEventProcessor {

        private final String projectKey;
        private final String issueKey;

        public InternalUpdateIssue(String projectKey, String issueKey) {
            this.projectKey = projectKey;
            this.issueKey = issueKey;
        }

        @Override
        public String getDescription() {
            return "InternalUpdateIssue - projectKey: " + projectKey + " issue: " + issueKey;
        }

        @Override
        public void processEvent() {
            issuesUpdatedByEvent.add(new IssueUpdate(cardsRepo.get(issueKey), IssueUpdateType.UPDATED));
        }
    }
    
    public void reset() {
        state = IssueBufferState.uninitialised;
        cardsRepo.clear();
        updateIssueBuffer();
        while (isUpdatingTaskboardIssuesBuffer)
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                return;
            }
    }

    public class ReorderIssuesAction implements Callable<List<Issue>> {

        private final String[] issueKeys;

        public ReorderIssuesAction(String[] issueKeys) {
            this.issueKeys = issueKeys;
        }

        @Override
        public List<Issue> call() {
            validateUserPermissions();

            List<TaskboardIssue> updated = issuePriorityService.reorder(issueKeys);
            updateIssuesPriorities(updated);

            List<Issue> updatedIssues = new LinkedList<>();
            for (String issue : issueKeys)
                updatedIssues.add(getIssueByKey(issue));

            return updatedIssues;
        }

        private void validateUserPermissions() {
            Set<String> projectsWithoutPermission = Arrays.stream(issueKeys)
                    .map(IssueBufferService.this::getIssueByKey)
                    .map(Issue::getProjectKey)
                    .distinct()
                    .filter(projectKey -> !authorizer.hasPermissionInProject(ADMINISTRATIVE, projectKey))
                    .collect(Collectors.toSet());
            if(!projectsWithoutPermission.isEmpty()) {
                throw new FrontEndMessageException("User doesn't have permission to reoder issues of " + projectsWithoutPermission);
            }
        }
    }
}
