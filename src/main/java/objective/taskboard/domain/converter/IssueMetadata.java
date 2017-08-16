package objective.taskboard.domain.converter;

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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;

import com.atlassian.jira.rest.client.api.domain.BasicComponent;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.IssueLink;
import com.atlassian.jira.rest.client.api.domain.IssueLinkType.Direction;

import lombok.Data;
import objective.taskboard.data.CustomField;
import objective.taskboard.jira.JiraProperties;

@Data
public class IssueMetadata {

    private final JiraProperties jiraProperties;
    private final Logger log;

    private final String projectKey;
    private final String assignee;
    private final String reporter;
    private final IssueParent realParent;
    private final List<IssueCoAssignee> coAssignees;
    private final CustomField classOfService;
    private final String blocked;
    private final String lastBlockReason;
    private final Map<String, CustomField> tShirtSizes;
    private final List<String> comments;
    private final List<String> dependenciesIssuesKey;
    private final String linkedParentKey;
    private final Map<String, CustomField> additionalEstimatedHours;
    private final Map<String, CustomField> release;
    private final List<String> labels;
    private final List<String> components;

    public IssueMetadata(Issue issue, JiraProperties jiraProperties, List<String> parentIssueLinks, Logger log) {
        this.jiraProperties = jiraProperties;
        this.log = log;
        this.projectKey = issue.getProject().getKey();
        this.assignee = issue.getAssignee() == null ? null : issue.getAssignee().getName();
        this.reporter = issue.getReporter() == null ? null : issue.getReporter().getName();
        this.realParent = extractRealParent(issue);
        this.linkedParentKey = extractLinkedParentKey(issue, parentIssueLinks);
        this.coAssignees = extractCoAssignees(issue);
        this.classOfService = extractClassOfService(issue);
        this.blocked = extractBlocked(issue);
        this.lastBlockReason = extractLastBlockReason(issue);
        this.tShirtSizes = extractTShirtSizes(issue);
        this.comments = extractComments(issue);
        this.dependenciesIssuesKey = extractDependenciesIssues(issue);
        this.additionalEstimatedHours = extractAdditionalEstimatedHours(issue);
        this.release = extractRelease(issue);
        this.labels = extractLabels(issue);
        this.components = extractComponents(issue);
    }

    private IssueParent extractRealParent(Issue issue) {
        IssueField field = issue.getField("parent");

        if (field == null)
            return null;

        JSONObject json = (JSONObject) field.getValue();

        if (json == null)
            return null;

        try {
            String key = json.getString("key");
            JSONObject type = json.getJSONObject("fields").getJSONObject("issuetype");
            long typeId = type.getLong("id");
            String typeIconUrl = type.getString("iconUrl");
            return new IssueParent(key, typeId, typeIconUrl);
        } catch (JSONException e) {
            logErrorExtractField(issue, field, e);
            return null;
        }
    }

    private String extractLinkedParentKey(Issue issue, List<String> parentIssueLinks) {
        if (issue.getIssueLinks() == null || issue.getIssueType().getId() == jiraProperties.getIssuetype().getDemand().getId())
            return null;

        List<IssueLink> links = newArrayList(issue.getIssueLinks()).stream()
                .filter(l -> parentIssueLinks.contains(l.getIssueLinkType().getDescription()))
                .collect(toList());

        if (links.isEmpty())
            return null;

        return links.get(0).getTargetIssueKey();
    }

    private List<IssueCoAssignee> extractCoAssignees(Issue issue) {
        IssueField field = issue.getField(jiraProperties.getCustomfield().getCoAssignees().getId());

        if (field == null)
            return newArrayList();

        JSONArray value = (JSONArray) field.getValue();

        if (value == null)
            return newArrayList();

        List<IssueCoAssignee> coAssignees = newArrayList();
        for (int i = 0; i < value.length(); i++) {
            try {
                String name = value.getJSONObject(i).getString("name");
                String avatarUrl = value.getJSONObject(i).getJSONObject("avatarUrls").getString("24x24");
                coAssignees.add(new IssueCoAssignee(name, avatarUrl));
            } catch (JSONException e) {
                logErrorExtractField(issue, field, e);
            }
        }

        return coAssignees;
    }

    private CustomField extractClassOfService(Issue issue) {
        IssueField field = issue.getField(jiraProperties.getCustomfield().getClassOfService().getId());

        if (field == null)
            return null;

        JSONObject json = (JSONObject) field.getValue();

        if (json == null)
            return null;

        try {
            Long optionId = json.getLong("id");
            String value = json.getString("value");
            return new CustomField(field.getName(), value, optionId);
        } catch (JSONException e) {
            logErrorExtractField(issue, field, e);
            return null;
        }
    }

    private String extractBlocked(Issue issue) {
        IssueField field = issue.getField(jiraProperties.getCustomfield().getBlocked().getId());

        if (field == null)
            return "";

        JSONArray jsonArray = (JSONArray) field.getValue();

        if (jsonArray == null || jsonArray.length() == 0)
            return "";

        try {
            return jsonArray.getJSONObject(0).getString("value");
        } catch (JSONException e) {
            logErrorExtractField(issue, field, e);
            return "";
        }
    }

    private String extractLastBlockReason(Issue issue) {
        IssueField field = issue.getField(jiraProperties.getCustomfield().getLastBlockReason().getId());

        if (field == null || field.getValue() == null)
            return "";

        String lastBlockReason = field.getValue().toString();
        return lastBlockReason.length() > 200 ? lastBlockReason.substring(0, 200) + "..." : lastBlockReason;
    }

    private Map<String, CustomField> extractTShirtSizes(Issue issue) {
        Map<String, CustomField> tShirtSizes = newHashMap();

        for (String tSizeId : jiraProperties.getCustomfield().getTShirtSize().getIds()) {
            String tShirtSizeValue = extractTShirtSize(issue, tSizeId);

            if (isNullOrEmpty(tShirtSizeValue))
                continue;

            String fieldName = issue.getField(tSizeId).getName();
            CustomField tShirtSize = new CustomField(fieldName, tShirtSizeValue);
            tShirtSizes.put(tSizeId, tShirtSize);
        }

        return tShirtSizes;
    }

    private String extractTShirtSize(Issue issue, String tShirtSizeId) {
        IssueField field = issue.getField(tShirtSizeId);

        if (field == null)
            return "";

        JSONObject json = (JSONObject) field.getValue();

        try {
            return json != null ? json.getString("value") : "";
        } catch (JSONException e) {
            logErrorExtractField(issue, field, e);
            return "";
        }
    }

    private List<String> extractComments(Issue issue) {
        if (issue.getComments() == null)
            return newArrayList();

        return newArrayList(issue.getComments()).stream()
               .map(Comment::toString)
               .collect(toList());
    }

    private List<String> extractDependenciesIssues(Issue issue) {
        if (issue.getIssueLinks() == null)
            return newArrayList();

        return newArrayList(issue.getIssueLinks()).stream()
                .filter(this::isDependencyLink)
                .map(link -> link.getTargetIssueKey())
                .collect(toList());
    }

    private boolean isDependencyLink(final IssueLink link) {
        return jiraProperties.getIssuelink().getDependencies().contains(link.getIssueLinkType().getName())
            && link.getIssueLinkType().getDirection() == Direction.OUTBOUND;
    }

    private Map<String, CustomField> extractAdditionalEstimatedHours(Issue issue) {
        String additionalHoursId = jiraProperties.getCustomfield().getAdditionalEstimatedHours().getId();
        IssueField field = issue.getField(additionalHoursId);
        if (field == null || field.getValue() == null)
            return newHashMap();

        Double additionalHours = (Double) field.getValue();
        CustomField customFieldAdditionalHours = new CustomField(field.getName(), additionalHours);
        Map<String, CustomField> mapAdditionalHours = newHashMap();
        mapAdditionalHours.put(additionalHoursId, customFieldAdditionalHours);
        return mapAdditionalHours;
    }

    private Map<String, CustomField> extractRelease(Issue issue) {
        String releaseId = jiraProperties.getCustomfield().getRelease().getId();
        IssueField field = issue.getField(releaseId);

        if (field == null)
            return newHashMap();

        JSONObject json = (JSONObject) field.getValue();

        if (json == null)
            return newHashMap();

        try {
            String release = json.getString("name");
            CustomField customFieldRelease = new CustomField(field.getName(), release);
            Map<String, CustomField> mapRelease = newHashMap();
            mapRelease.put(releaseId, customFieldRelease);
            return mapRelease;
        } catch (JSONException e) {
            logErrorExtractField(issue, field, e);
            return newHashMap();
        }
    }

    private List<String> extractLabels(Issue issue) {
        if (issue.getLabels() == null)
            return newArrayList();

        return issue.getLabels().stream().collect(toList());
    }

    private List<String> extractComponents(Issue issue) {
        if (issue.getComponents() == null)
            return newArrayList();

        return newArrayList(issue.getComponents()).stream()
                .map(BasicComponent::getName)
                .collect(toList());
    }

    private void logErrorExtractField(Issue issue, IssueField field, JSONException e) {
        log.error("Error extracting " + field.getName() + " from issue " + issue.getKey() + ": " + e.getMessage(), e);
    }

    public String getParentKey() {
        if (getRealParent() != null)
            return getRealParent().getKey();
        if (getLinkedParentKey() != null)
            return getLinkedParentKey();
        return "";
    }

    @Data
    static class IssueParent {
        private final String key;
        private final long typeId;
        private final String typeIconUrl;

        public IssueParent(String key, long issueTypeId, String issueTypeIconUrl) {
            this.key = key;
            this.typeId = issueTypeId;
            this.typeIconUrl = issueTypeIconUrl;
        }
    }

    @Data
    static class IssueCoAssignee {
        private final String name;
        private final String avatarUrl;

        public IssueCoAssignee(String name, String avatarUrl) {
            this.name = name;
            this.avatarUrl = avatarUrl;
        }
    }

}