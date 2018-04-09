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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import objective.taskboard.issueBuffer.IssueBufferService;
import objective.taskboard.jira.JiraServiceUnavailable;

@Component
public class IssueEventProcessScheduler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IssueEventProcessScheduler.class);
    @Autowired
    private IssueBufferService issueBufferService;

    List<JiraEventProcessor> list = Collections.synchronizedList(new ArrayList<>());

    private synchronized List<JiraEventProcessor> getItens() {
        return new ArrayList<>(list);
    }

    private synchronized void removeItens(List<JiraEventProcessor> list) {
        this.list.removeAll(list);
    }

    public synchronized void add(JiraEventProcessor eventProcessor) {
        list.add(eventProcessor);
    }

    public void processItems() {
        List<JiraEventProcessor> toRemove = new ArrayList<>();

        issueBufferService.startBatchUpdate();
        for (JiraEventProcessor item : getItens())
            try {
                item.processEvent();

                toRemove.add(item);
            }
            catch(JiraServiceUnavailable ex) {
                log.warn("WEBHOOK PROCESS FAILED FOR ITEM: " + item.getDescription());
                log.warn("Jira was not available. Will retry later", ex);
            }
            catch (Exception ex) {
                log.warn("WEBHOOK PROCESS FAILED: " + item.getDescription());
                log.error("WebhookScheduleError, unrecoverable error. Event will be discarded:", ex);
                toRemove.add(item);
            }

        issueBufferService.finishBatchUpdate();
        removeItens(toRemove);
    }
}
