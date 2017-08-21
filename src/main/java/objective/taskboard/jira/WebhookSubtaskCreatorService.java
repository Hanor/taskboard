package objective.taskboard.jira;

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

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import objective.taskboard.controller.WebhookController.WebhookBody.Changelog;
import objective.taskboard.jira.JiraProperties.SubtaskCreation;

@Service
public class WebhookSubtaskCreatorService {

    private final SubtaskCreatorService subtaskCreatorService;
    private final JiraProperties jiraProperties;
    
    public WebhookSubtaskCreatorService(SubtaskCreatorService subtaskCreatorService, JiraProperties jiraProperties) {
        this.subtaskCreatorService = subtaskCreatorService;
        this.jiraProperties = jiraProperties;
    }

    public void createSubtaskOnTransition(com.atlassian.jira.rest.client.api.domain.Issue parent, Changelog changelog) {
        if (changelog == null || parent == null)
            return;
        
        Optional<Map<String, Object>> statusChangeOpt = changelog.items.stream()
                .filter(i -> i.containsKey("field") && "status".equals(i.get("field")))
                .findFirst();
        
        if (!statusChangeOpt.isPresent())
            return;
        
        Long statusIdFrom = Long.parseLong((String) statusChangeOpt.get().get("from"));
        Long statusIdTo = Long.parseLong((String) statusChangeOpt.get().get("to"));
        
        Optional<SubtaskCreation> properties = propertiesFor(parent.getIssueType().getId(), statusIdFrom, statusIdTo);
        properties.ifPresent(p -> subtaskCreatorService.create(parent, p));
    }
    
    private Optional<SubtaskCreation> propertiesFor(Long issueTypeId, Long statusIdFrom, Long statusIdTo) {
        return jiraProperties.getSubtaskCreation().stream()
                .filter(p -> p.getIssueTypeParentId().equals(issueTypeId)
                        && p.getStatusIdFrom().equals(statusIdFrom)
                        && p.getStatusIdTo().equals(statusIdTo))
                .findFirst();
    }

}
