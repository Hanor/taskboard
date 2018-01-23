package objective.taskboard.domain.converter;

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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.domain.Filter;
import objective.taskboard.domain.Step;
import objective.taskboard.jira.client.ChangelogGroupDto;
import objective.taskboard.jira.client.ChangelogItemDto;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.repository.FilterCachedRepository;

@Service
public class StartDateStepService {

    @Autowired
    private FilterCachedRepository filterRepository;

    public long get(JiraIssueDto jiraIssue) {
        if (jiraIssue == null)
            return 0;

        if (jiraIssue.getChangelog() == null)
            return jiraIssue.getCreationDate().getMillis();

        List<ChangelogGroupDto> changelogStatus = getChangelogsStatus(jiraIssue.getChangelog());

        if (changelogStatus.isEmpty())
            return jiraIssue.getCreationDate().getMillis();

        List<Filter> filtersIssueType = filterRepository.getCache().stream()
                                            .filter(f -> f.getIssueTypeId() == jiraIssue.getIssueType().getId())
                                            .collect(toList());

        List<Step> stepsIssue = filtersIssueType.stream()
                                    .filter(f -> f.getStatusId() == jiraIssue.getStatus().getId())
                                    .map(f -> f.getStep())
                                    .collect(toList());

        Map<Step, List<Long>> statusSteps = filtersIssueType.stream()
                                                .filter(f -> stepsIssue.contains(f.getStep()))
                                                .collect(groupingBy(Filter::getStep,
                                                                    mapping(Filter::getStatusId, toList())));

        changelogStatus.sort(new Comparator<ChangelogGroupDto>() {
            @Override
            public int compare(ChangelogGroupDto c1, ChangelogGroupDto c2) {
                return -c1.getCreated().compareTo(c2.getCreated());
            }
        });

        ChangelogGroupDto firstStatusStep = getFirstStatusStep(changelogStatus, statusSteps);
        return firstStatusStep == null ?
                   jiraIssue.getCreationDate().getMillis() :
                   firstStatusStep.getCreated().getMillis();
    }

    private List<ChangelogGroupDto> getChangelogsStatus(Iterable<ChangelogGroupDto> changelog) {
        List<ChangelogGroupDto> changelogStatus = newArrayList();
        for (ChangelogGroupDto changelogGroup : changelog)
            for (ChangelogItemDto changelogItem : changelogGroup.getItems())
                if (changelogItem.getField().equals("status"))
                    changelogStatus.add(new ChangelogGroupDto(changelogGroup.getAuthor(),
                                                           changelogGroup.getCreated(),
                                                           newArrayList(changelogItem)));
        return changelogStatus;
    }

    private ChangelogGroupDto getFirstStatusStep(List<ChangelogGroupDto> changelogStatus, Map<Step, List<Long>> statusSteps) {
        ChangelogGroupDto firstStatusInStep = null;
        for (ChangelogGroupDto changelog : changelogStatus) {
            Long statusTo = Long.valueOf(newArrayList(changelog.getItems()).get(0).getTo());
            if (!isStatusStep(statusSteps, statusTo))
                break;
            firstStatusInStep = changelog;
        }
        return firstStatusInStep;
    }

    private boolean isStatusStep(Map<Step, List<Long>> statusSteps, Long status) {
        for (List<Long> statuses : statusSteps.values())
            if (statuses.contains(status))
                return true;
        return false;
    }

}
