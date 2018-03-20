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
package objective.taskboard.domain.converter;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import objective.taskboard.data.Changelog;
import objective.taskboard.data.CustomField;
import objective.taskboard.data.Worklog;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.client.JiraCommentDto;
import objective.taskboard.jira.client.JiraComponentDto;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.client.JiraIssueLinkTypeDto;
import objective.taskboard.jira.client.JiraLinkDto;
import objective.taskboard.jira.client.JiraWorklogResultSetDto;
import objective.taskboard.utils.DateTimeUtils;

public class IssueFieldsExtractor {
    private static final int REASON_WIDTH_LIMIT = 200;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IssueFieldsExtractor.class);

    public static String extractRealParent(JiraIssueDto issue) {
        JSONObject json = issue.getField("parent");

        if (json == null)
            return null;

        try {
            return json.getString("key");
        } catch (JSONException e) {
            logErrorExtractField(issue, "parent.key", e);
            return null;
        }
    }

    public static String extractParentKey(JiraProperties jiraProperties, JiraIssueDto issue, List<String> parentIssueLinks) {
    	String realParent = extractRealParent(issue);
        if (realParent != null)
            return realParent;
        
        String linkedParentKey = extractLinkedParentKey(jiraProperties, issue, parentIssueLinks);
        if (linkedParentKey != null)
            return linkedParentKey;
        return "";
    }

    public  static String extractLinkedParentKey(JiraProperties jiraProperties, JiraIssueDto issue, List<String> parentIssueLinks) {
        if (isEmpty(issue.getIssueLinks()) || issue.getIssueType().getId() == jiraProperties.getIssuetype().getDemand().getId())
            return null;

        List<JiraLinkDto> links = newArrayList(issue.getIssueLinks()).stream()
                .filter(l -> parentIssueLinks.contains(l.getIssueLinkType().getDescription()))
                .collect(toList());

        if (links.isEmpty())
            return null;

        return links.get(0).getTargetIssueKey();
    }

    public  static List<IssueCoAssignee> extractCoAssignees(JiraProperties jiraProperties, JiraIssueDto issue) {
        String fieldId = jiraProperties.getCustomfield().getCoAssignees().getId();
        JSONArray value = issue.getField(fieldId);

        if (value == null)
            return newArrayList();

        List<IssueCoAssignee> coAssignees = newArrayList();
        for (int i = 0; i < value.length(); i++) {
            try {
                String name = value.getJSONObject(i).getString("name");
                String avatarUrl = value.getJSONObject(i).getJSONObject("avatarUrls").getString("24x24");
                coAssignees.add(new IssueCoAssignee(name, avatarUrl));
            } catch (JSONException e) {
            	logErrorExtractField(issue, fieldId + ".name (co-assignees)", e);
            }
        }

        return coAssignees;
    }

    public static CustomField extractClassOfService(JiraProperties jiraProperties,JiraIssueDto issue) {
        String fieldId = jiraProperties.getCustomfield().getClassOfService().getId();
        JSONObject json = issue.getField(fieldId);

        if (json == null)
            return null;

        try {
            Long optionId = json.getLong("id");
            String value = json.getString("value");
            return new CustomField(fieldId, value, optionId);
        } catch (JSONException e) {
            logErrorExtractField(issue, fieldId + ".value (classOfService)", e);
            return null;
        }
    }

    public static boolean extractSingleValueCheckbox(String customFieldId, JiraIssueDto issue) {
        JSONArray jsonArray = issue.getField(customFieldId);

        if (jsonArray == null || jsonArray.length() == 0)
            return false;

        try {
            return isNotEmpty(jsonArray.getJSONObject(0).getString("value"));
        } catch (JSONException e) {
            logErrorExtractField(issue, customFieldId + ".value", e);
            return false;
        }
    }

    public  static boolean extractBlocked(JiraProperties jiraProperties, JiraIssueDto issue) {
        JSONArray jsonArray = issue.getField(jiraProperties.getCustomfield().getBlocked().getId());

        if (jsonArray == null || jsonArray.length() == 0)
            return false;

        try {
            return jsonArray.getJSONObject(0) != null 
                   && jsonArray.getJSONObject(0).getInt("id") == jiraProperties.getCustomfield().getBlocked().getYesOptionId();
        } catch (JSONException e) {
            logErrorExtractField(issue, jiraProperties.getCustomfield().getBlocked().getId() + ".value", e);
            return false;
        }
    }

    public  static String extractLastBlockReason(JiraProperties jiraProperties, JiraIssueDto issue) {
        String lastBlockReason = issue.getField(jiraProperties.getCustomfield().getLastBlockReason().getId());

        if (isEmpty(lastBlockReason))
            return "";

        return lastBlockReason.length() > REASON_WIDTH_LIMIT ? lastBlockReason.substring(0, REASON_WIDTH_LIMIT) + "..." : lastBlockReason;
    }

    public  static Map<String, CustomField> extractTShirtSizes(JiraProperties jiraProperties, JiraIssueDto issue) {
        Map<String, CustomField> tShirtSizes = newHashMap();

        for (String tSizeId : jiraProperties.getCustomfield().getTShirtSize().getIds()) {
            String tShirtSizeValue = extractTShirtSize(issue, tSizeId);

            if (isNullOrEmpty(tShirtSizeValue))
                continue;

            CustomField tShirtSize = new CustomField(tSizeId, tShirtSizeValue);
            tShirtSizes.put(tSizeId, tShirtSize);
        }

        return tShirtSizes;
    }

    public  static String extractTShirtSize(JiraIssueDto issue, String tShirtSizeId) {
        JSONObject json = issue.getField(tShirtSizeId);

        try {
            return json != null ? json.getString("value") : "";
        } catch (JSONException e) {
            logErrorExtractField(issue, tShirtSizeId + ".value (t-shirt size)", e);
            return "";
        }
    }

    public  static List<String> extractComments(JiraIssueDto issue) {
        if (issue.getComments() == null)
            return newArrayList();

        return newArrayList(issue.getComments()).stream()
               .map(JiraCommentDto::toString)
               .collect(toList());
    }

    public  static List<String> extractDependenciesIssues(JiraProperties jiraProperties, JiraIssueDto issue) {
        if (isEmpty(issue.getIssueLinks()))
            return newArrayList();

        List<String> dependencies = jiraProperties.getIssuelink().getDependencies();
        
        return newArrayList(issue.getIssueLinks()).stream()
                .filter(link ->{
					return dependencies.contains(link.getIssueLinkType().getName()) && link.getIssueLinkType().getDirection() == JiraIssueLinkTypeDto.Direction.OUTBOUND;
				})
                .map(link -> link.getTargetIssueKey())
                .collect(toList());
    }


    public static CustomField extractAdditionalEstimatedHours(JiraProperties jiraProperties, JiraIssueDto issue) {
        String additionalHoursId = jiraProperties.getCustomfield().getAdditionalEstimatedHours().getId();
        Double additionalHours = issue.getField(additionalHoursId);
        if (additionalHours == null)
            return null;

        return new CustomField(additionalHoursId, additionalHours);
    }

    public static String extractReleaseId(JiraProperties jiraProperties, JiraIssueDto issue) {
        String releaseFieldId = jiraProperties.getCustomfield().getRelease().getId();
        JSONObject json = issue.getField(releaseFieldId);

        if (json == null)
            return null;

        try {
            return json.getString("id");
        } catch (JSONException e) {
            logErrorExtractField(issue, releaseFieldId + ".id (release)", e);
            return null;
        }
    }

    public static List<String> extractLabels(JiraIssueDto issue) {
        if (issue.getLabels() == null)
            return newArrayList();

        return issue.getLabels().stream().collect(toList());
    }

    public static List<String> extractComponents(JiraIssueDto issue) {
        if (issue.getComponents() == null)
            return newArrayList();

        return newArrayList(issue.getComponents()).stream()
                .map(JiraComponentDto::getName)
                .collect(toList());
    }

    public static List<Changelog> extractChangelog(JiraIssueDto issue) {
        if (issue.getChangelog() == null)
            return Collections.emptyList();

        List<Changelog> result = new LinkedList<>();
        issue.getChangelog().forEach(change -> {
            change.getItems().forEach(item -> {
                result.add(new Changelog(
                        change.getAuthor().getName(), 
                        item.getField(), 
                        item.getFromString(), 
                        item.getToString(), 
                        item.getTo(),
                        DateTimeUtils.get(change.getCreated())));
            });
        });
        result.sort((item1, item2) -> item1.timestamp.compareTo(item2.timestamp));
        return result;
    }


    public static List<Long> extractAssignedTeamsIds(JiraProperties jiraProperties, JiraIssueDto issue) {
        String valueString = issue.getField(jiraProperties.getCustomfield().getAssignedTeams().getId());

        if (valueString == null || valueString.isEmpty())
            return newArrayList();

        return Arrays.asList(valueString.split(",")).stream().map(v -> Long.parseLong(v)).collect(Collectors.toList());
    }

    private static void logErrorExtractField(JiraIssueDto issue, String fieldName, JSONException e) {
        log.error("Error extracting " + fieldName + " from issue " + issue.getKey() + ": " + e.getMessage());
    }

    public static List<Worklog> convertWorklog(JiraWorklogResultSetDto jiraWorklogs) {
        List<Worklog> worklogs = jiraWorklogs.worklogs.stream()
            .map(Worklog::from)
            .collect(Collectors.toList());

        return worklogs;
    }
}
