package objective.taskboard.domain;

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

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import objective.taskboard.config.CacheConfiguration;
import objective.taskboard.database.TaskboardDatabaseService;
import objective.taskboard.jira.JiraProperties;

@Service
public class IssueColorService {

    @Autowired
    private JiraProperties jiraProperties;
    
    @Autowired
    private TaskboardDatabaseService taskboardDatabaseService;
    
    public String getColor(Long classOfServiceId) {
        Map<Long, String> colors = jiraProperties.getCustomfield().getClassOfService().getColors();
        String color = colors == null ? null : colors.get(classOfServiceId);

        if (color != null)
            return color;

        return "#DDF9D9";
    }

    @Cacheable(CacheConfiguration.COLOR_BY_ISSUETYPE_AND_STATUS)
    public String getStatusColor(Long issueType, Long status) {
        Map<Pair<Long, Long>, String> colorByIssueTypeAndStatus = taskboardDatabaseService.getColorByIssueTypeAndStatus();
        return colorByIssueTypeAndStatus.get(Pair.of(issueType, status));
    }
}
