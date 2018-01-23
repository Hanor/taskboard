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

package objective.taskboard.jira;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.data.WebHookBody;

@Service
public class WebhookSubtaskCreatorService {

    private final SubtaskCreatorService subtaskCreatorService;
    private final JiraProperties jiraProperties;

    public WebhookSubtaskCreatorService(SubtaskCreatorService subtaskCreatorService, JiraProperties jiraProperties) {
        this.subtaskCreatorService = subtaskCreatorService;
        this.jiraProperties = jiraProperties;
    }

    public void createSubtaskOnTransition(JiraIssueDto parent, WebHookBody.Changelog changelog) {
        if (changelog == null || parent == null)
            return;
        
        Optional<Map<String, Object>> statusChangeOpt = changelog.items.stream()
                .filter(i -> i.containsKey("field") && "status".equals(i.get("field")))
                .findFirst();
        
        if (!statusChangeOpt.isPresent())
            return;
        
        Long statusIdFrom = Long.parseLong((String) statusChangeOpt.get().get("from"));
        Long statusIdTo = Long.parseLong((String) statusChangeOpt.get().get("to"));
        Long issueTypeId = parent.getIssueType().getId();

        jiraProperties.getSubtaskCreation().stream()
            .filter(p -> p.getIssueTypeParentId().equals(issueTypeId)
                    && p.getStatusIdFrom().equals(statusIdFrom)
                    && p.getStatusIdTo().equals(statusIdTo))
            .forEach(p -> subtaskCreatorService.create(parent, p));
    }

}
