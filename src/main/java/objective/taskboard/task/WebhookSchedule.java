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
package objective.taskboard.task;

import static objective.taskboard.config.CacheConfiguration.PROJECTS;
import static objective.taskboard.issueBuffer.WebhookEvent.VERSION_UPDATED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import objective.taskboard.data.Issue;
import objective.taskboard.issue.IssueUpdate;
import objective.taskboard.issue.IssueUpdateType;
import objective.taskboard.issue.IssuesUpdateEvent;
import objective.taskboard.issueBuffer.IssueBufferService;
import objective.taskboard.issueBuffer.WebhookEvent;
import objective.taskboard.jira.JiraServiceUnavailable;

@Slf4j
@Component
public class WebhookSchedule {
    @Autowired
    private IssueBufferService issueBufferService;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    public ApplicationEventPublisher eventPublisher;

    private static final long RATE_MILISECONDS = 1 * 1000;
    
    List<Item> list = Collections.synchronizedList(new ArrayList<Item>());
    
    private class Item {
        private WebhookEvent event;
        private String issueKey;

        public Item(WebhookEvent event, String issueKey) {
            this.event = event;
            this.issueKey = issueKey;
        }
    }

    private synchronized List<Item> getItens() {
        return new ArrayList<Item>(list);
    }

    private synchronized void removeItens(List<Item> list) {
        this.list.removeAll(list);
    }

    public void add(WebhookEvent event, String issueKey) {
        Item item = new Item(event, issueKey);
        list.add(item);
    }

    @Scheduled(fixedRate = RATE_MILISECONDS)
    public void processItems() {
        List<Item> toRemove = new ArrayList<Item>();
        List<IssueUpdate> updatedIssues = new ArrayList<>();

        for (Item item : getItens()) 
            try {
                if (item.event.isTypeVersion()) {
                    cacheManager.getCache(PROJECTS).clear();
                    if (item.event.equals(VERSION_UPDATED))
                        issueBufferService.updateIssueBuffer();
                } else {
                    IssueUpdateType updateType = computeEventType(item);
                    
                    Issue issueToUpdate = issueBufferService.getIssueByKey(item.issueKey);
                    
                    issueBufferService.updateIssueBuffer(item.event, item.issueKey);
                    
                    Issue updatedIssue = issueBufferService.getIssueByKey(item.issueKey);
                    if (updatedIssue == null) // just in case the issue was removed
                        updatedIssue = issueToUpdate;
                    
                    updatedIssues.add(new IssueUpdate(updatedIssue, updateType));
                }

                log.warn("WEBHOOK PROCESSED: (" + item.event.toString() +  ") issue=" + item.issueKey);
                toRemove.add(item);
            }
            catch(JiraServiceUnavailable ex) {
                log.warn("WEBHOOK PROCESS FAILED FOR ITEM: (" + item.event.toString() +  ") issue=" + item.issueKey);
                log.warn("Jira was not available. Will retry later", ex);
            }
            catch (Exception ex) {
                log.warn("WEBHOOK PROCESS FAILED: (" + item.event.toString() +  ") issue=" + item.issueKey);
                log.error("WebhookScheduleError, unrecoverable error. Event will be discarded:", ex);
                toRemove.add(item);
            }
        
        if (updatedIssues.size() > 0)
            eventPublisher.publishEvent(new IssuesUpdateEvent(this, updatedIssues));

        removeItens(toRemove);
    }

    private IssueUpdateType computeEventType(Item item) {
        IssueUpdateType updateType;
        switch(item.event){
            case ISSUE_CREATED:
                updateType = IssueUpdateType.CREATED;
                break;
            case ISSUE_DELETED:
                updateType = IssueUpdateType.DELETED;
                break;
            case ISSUE_UPDATED:
                updateType = IssueUpdateType.UPDATED;
                break;
            default:
                updateType = IssueUpdateType.UNKNOWN;
        }
        return updateType;
    }

}
