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
package objective.taskboard.controller;

import static java.util.Optional.ofNullable;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import objective.taskboard.jira.data.WebHookBody;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.task.IssueEventProcessScheduler;
import objective.taskboard.task.JiraEventProcessor;
import objective.taskboard.task.JiraEventProcessorFactory;

@RestController
@RequestMapping("webhook")
public class WebhookController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private ProjectFilterConfigurationCachedRepository projectRepository;

    @Autowired
    private IssueEventProcessScheduler webhookSchedule;

    @Autowired
    private List<JiraEventProcessorFactory> jiraEventProcessorFactories;

    @RequestMapping(value = "{projectKey}", method = RequestMethod.POST)
    public void webhook(@RequestBody WebHookBody body, @PathVariable("projectKey") String projectKey) throws JsonProcessingException {
        log.debug("Incoming Webhook request: type " + 
                body.webhookEvent.typeName +
                " / timestamp " + body.timestamp +
                " / issue: " +   ofNullable(body.issue).map(i->i.get("key")).orElse("N/A") +
                " / version: " + ofNullable(body.version).map(t->t.name).orElse("N/A") +
                " / user: " +    ofNullable(body.user).map(u->u.get("name")).orElse("N/A"));

        if(body.webhookEvent == null)
            return;

        if(!belongsToAnyProject(projectKey))
            return;

        for(JiraEventProcessorFactory factory : jiraEventProcessorFactories) {
            factory.create(body, projectKey)
                    .ifPresent(this::enqueue);
        }
    }

    private void enqueue(JiraEventProcessor eventProcessor) {
        webhookSchedule.add(eventProcessor);
    }

    protected boolean belongsToAnyProject(String projectKey) {
        return projectRepository.exists(projectKey);
    }
}
