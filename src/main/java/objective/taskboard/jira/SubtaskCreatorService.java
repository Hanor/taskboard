/*- 
 * [LICENSE] 
 * Taskboard 
 * - - - 
 * Copyright (C) 2015 - 2017 Objective Solutions 
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

import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractSingleValueCheckbox;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.atlassian.jira.rest.client.api.domain.BasicUser;
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;

import objective.taskboard.jira.JiraProperties.SubtaskCreation;
import objective.taskboard.jira.JiraProperties.SubtaskCreation.CustomFieldCondition;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.client.JiraIssueFieldDto;
import objective.taskboard.jira.client.JiraSubtaskDto;
import objective.taskboard.jira.client.JiraUserDto;
import objective.taskboard.jira.data.Transition;

@Service
public class SubtaskCreatorService {

    private static final Logger log = LoggerFactory.getLogger(SubtaskCreatorService.class);

    private final JiraService jiraService;
    private final JiraProperties jiraProperties;

    public SubtaskCreatorService(JiraService jiraService, JiraProperties jiraProperties) {
        this.jiraService = jiraService;
        this.jiraProperties = jiraProperties;
    }

    public void create(JiraIssueDto parent, SubtaskCreation creationProperties) {
        if (!hasRequiredValueOrHasNoRequirement(parent, creationProperties))
            return;
        
        if(skipCreationWhenTShirtSizeParentIsAbsent(parent, creationProperties))
            return;

        Long typeId = creationProperties.getIssueTypeId();

        String subtaskKey = subtaskOfType(parent, typeId);        
        if (subtaskKey == null) {
            subtaskKey = create(parent, creationProperties, typeId);
        } else {
            log.debug("Subtask already exists. Creation will be skipped");
        }

        Optional<Long> transitionId = creationProperties.getTransitionId();
        if (transitionId.isPresent())
            executeTransitionIfAvailable(transitionId.get(), subtaskKey);
    }

    private boolean hasRequiredValueOrHasNoRequirement(JiraIssueDto parent, SubtaskCreation creationProperties) {
        final Optional<CustomFieldCondition> customFieldCondition = creationProperties.getCustomFieldCondition();
        if (customFieldCondition.isPresent()) {
            final String currentValue = extractSingleValueCheckbox(customFieldCondition.get().getId(), parent);
            if (!customFieldCondition.get().getValue().equals(currentValue))
                return false;
        }
        return true;
    }
    
    private boolean skipCreationWhenTShirtSizeParentIsAbsent(JiraIssueDto parent, SubtaskCreation creationProperties) {
        JiraIssueFieldDto parentTShirtSize = parent.getField(creationProperties.getTShirtSizeParentId());
        Boolean skipCreation = creationProperties.getSkipCreationWhenTShirtParentIsAbsent();
        return (parentTShirtSize == null || parentTShirtSize.getValue() == null) && skipCreation;
    }

    private String subtaskOfType(JiraIssueDto parent, Long typeId) {
        for (JiraSubtaskDto sub : parent.getSubtasks())
            if (sub.getIssueType().getId().equals(typeId))
                return sub.getIssueKey();
        return null;
    }

    private String create(JiraIssueDto parent, SubtaskCreation creationProperties, Long typeId) {
        String summaryPrefix = creationProperties.getSummaryPrefix();
        String tShirtSizeParentId = creationProperties.getTShirtSizeParentId();
        String tShirtSizeSubtaskId = creationProperties.getTShirtSizeSubtaskId();
        String tShirtSizeDefaultValue = creationProperties.getTShirtSizeDefaultValue();

        IssueInputBuilder issueBuilder = new IssueInputBuilder(parent.getProject().getKey(), typeId);
        issueBuilder.setFieldValue("parent", ComplexIssueInputFieldValue.with("key", parent.getKey()));
        issueBuilder.setPriorityId(parent.getPriority().getId());
        issueBuilder.setSummary(summaryPrefix + parent.getSummary());
        issueBuilder.setReporter(toJiraStandardUser(parent.getReporter()));
        setTShirtSize(issueBuilder, parent, tShirtSizeParentId, tShirtSizeSubtaskId, tShirtSizeDefaultValue);
        setClassOfService(issueBuilder, parent);
        
        IssueInput issueInput = issueBuilder.build();
        
        log.debug("Creating subtask of issue " + parent.getKey() + ". Type id: " + typeId);

        return jiraService.createIssueAsMaster(issueInput);
    }

    private BasicUser toJiraStandardUser(JiraUserDto reporter) {
        try {
            return new BasicUser(new URI(reporter.getSelf()), reporter.getName(), reporter.getDisplayName());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Reporter has malformed self URL", e);
        }
    }

    private void setTShirtSize(IssueInputBuilder issueBuilder, JiraIssueDto parent, String tShirtParentId, String tShirtSubtaskId, String defaultValue) {
        String tShirtValue = getTShirtSizeValue(parent, tShirtParentId, defaultValue);
        issueBuilder.setFieldValue(tShirtSubtaskId, ComplexIssueInputFieldValue.with("value", tShirtValue));
    }

    private String getTShirtSizeValue(JiraIssueDto parent, String tShirtSizeParentId, String tShirtSizeDefaultValue) {
        JiraIssueFieldDto parentTShirtSize = parent.getField(tShirtSizeParentId);
        if (parentTShirtSize == null || parentTShirtSize.getValue() == null)
            return tShirtSizeDefaultValue;

        try {
            return ((JSONObject) parentTShirtSize.getValue()).getString("value");
        } catch (JSONException e) {
            log.error("Error extracting t-shirt-size value (customfield id = '" + tShirtSizeParentId + "') from parent issue '" + parent.getKey() + "'", e);
            return  tShirtSizeDefaultValue;
        }
    }

    private void setClassOfService(IssueInputBuilder issueBuilder, JiraIssueDto parent) {
        String classOfServiceId = jiraProperties.getCustomfield().getClassOfService().getId();
        JiraIssueFieldDto parentClassOfService = parent.getField(classOfServiceId);
        
        if (parentClassOfService == null || parentClassOfService.getValue() == null)
            return;

        try {
            String valueId = ((JSONObject) parentClassOfService.getValue()).getString("id");
            issueBuilder.setFieldValue(classOfServiceId, ComplexIssueInputFieldValue.with("id", valueId));
        } catch (JSONException e) {
            // just don't set if can't extract parent value
            log.error("Error extracting class-of-service value (customfield id = '" + classOfServiceId + "') from parent issue '" + parent.getKey() + "'", e);
        }
    }

    private void executeTransitionIfAvailable(Long transitionId, String issueKey) {
        Optional<Transition> transition = findTransition(issueKey, transitionId);
        if (!transition.isPresent()) {
            log.debug("Transition " + transitionId + " is not available for issue " + issueKey);
            return;
        }

        log.debug("Executing transition of subtask " + issueKey + ". Transition id: " + transitionId);

        try {
            jiraService.doTransitionAsMaster(issueKey, transitionId);
        } catch (JiraServiceException e) {
            log.error("Error executing transition '" + transitionId + "' on issue '" + issueKey + "'", e);
        }
    }

    private Optional<Transition> findTransition(String issueKey, Long transitionId) {
        List<Transition> transitions = jiraService.getTransitionsAsMaster(issueKey);
        return transitions.stream()
                .filter(t -> transitionId.equals(t.id))
                .findFirst();
    }

}
