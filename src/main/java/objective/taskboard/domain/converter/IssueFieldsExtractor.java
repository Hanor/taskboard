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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import objective.taskboard.data.Changelog;
import objective.taskboard.data.CustomField;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.client.JiraCommentDto;
import objective.taskboard.jira.client.JiraComponentDto;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.client.JiraIssueFieldDto;
import objective.taskboard.jira.client.JiraIssueLinkTypeDto;
import objective.taskboard.jira.client.JiraLinkDto;
import objective.taskboard.utils.DateTimeUtils;

public class IssueFieldsExtractor {
    private static final int REASON_WIDTH_LIMIT = 200;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IssueFieldsExtractor.class);

    public static IssueParent extractRealParent(JiraIssueDto issue) {
        JiraIssueFieldDto field = issue.getField("parent");

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
    
    public static String extractParentKey(JiraProperties jiraProperties, JiraIssueDto issue, List<String> parentIssueLinks) {
    	IssueParent realParent = extractRealParent(issue);
        if (realParent != null)
            return realParent.getKey();
        
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
        JiraIssueFieldDto field = issue.getField(jiraProperties.getCustomfield().getCoAssignees().getId());

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

    public static CustomField extractClassOfService(JiraProperties jiraProperties,JiraIssueDto issue) {
        JiraIssueFieldDto field = issue.getField(jiraProperties.getCustomfield().getClassOfService().getId());

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

    public static String extractSingleValueCheckbox(String customFieldId, JiraIssueDto issue) {
        JiraIssueFieldDto field = issue.getField(customFieldId);

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

    public  static String extractBlocked(JiraProperties jiraProperties, JiraIssueDto issue) {
        return extractSingleValueCheckbox(jiraProperties.getCustomfield().getBlocked().getId(), issue);
    }

    public  static String extractLastBlockReason(JiraProperties jiraProperties, JiraIssueDto issue) {
        JiraIssueFieldDto field = issue.getField(jiraProperties.getCustomfield().getLastBlockReason().getId());

        if (field == null || field.getValue() == null)
            return "";

        String lastBlockReason = field.getValue().toString();
        return lastBlockReason.length() > REASON_WIDTH_LIMIT ? lastBlockReason.substring(0, REASON_WIDTH_LIMIT) + "..." : lastBlockReason;
    }

    public  static Map<String, CustomField> extractTShirtSizes(JiraProperties jiraProperties, JiraIssueDto issue) {
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

    public  static String extractTShirtSize(JiraIssueDto issue, String tShirtSizeId) {
        JiraIssueFieldDto field = issue.getField(tShirtSizeId);

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


    public static Map<String, CustomField> extractAdditionalEstimatedHours(JiraProperties jiraProperties, JiraIssueDto issue) {
        String additionalHoursId = jiraProperties.getCustomfield().getAdditionalEstimatedHours().getId();
        JiraIssueFieldDto field = issue.getField(additionalHoursId);
        if (field == null || field.getValue() == null)
            return newHashMap();

        Double additionalHours = (Double) field.getValue();
        CustomField customFieldAdditionalHours = new CustomField(field.getName(), additionalHours);
        Map<String, CustomField> mapAdditionalHours = newHashMap();
        mapAdditionalHours.put(additionalHoursId, customFieldAdditionalHours);
        return mapAdditionalHours;
    }

    public static String extractReleaseId(JiraProperties jiraProperties, JiraIssueDto issue) {
        String releaseFieldId = jiraProperties.getCustomfield().getRelease().getId();
        JiraIssueFieldDto field = issue.getField(releaseFieldId);

        if (field == null)
            return null;

        JSONObject json = (JSONObject) field.getValue();

        if (json == null)
            return null;

        try {
            return json.getString("id");
        } catch (JSONException e) {
            logErrorExtractField(issue, field, e);
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

    private static void logErrorExtractField(JiraIssueDto issue, JiraIssueFieldDto field, JSONException e) {
        log.error("Error extracting " + field.getName() + " from issue " + issue.getKey() + ": " + e.getMessage());
    }

    private static boolean isEmpty(List<JiraLinkDto> issueLinks) {
        return issueLinks == null || issueLinks.isEmpty();
    }
}
