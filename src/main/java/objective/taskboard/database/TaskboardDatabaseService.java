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
package objective.taskboard.database;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import objective.taskboard.config.CacheConfiguration;
import objective.taskboard.data.LaneConfiguration;
import objective.taskboard.domain.Lane;
import objective.taskboard.repository.LaneCachedRepository;

@Service
public class TaskboardDatabaseService {

    @Autowired
    private LaneCachedRepository laneRepository;

    @Cacheable(CacheConfiguration.CONFIGURATION)
    public List<LaneConfiguration> laneConfiguration() {
        List<Lane> lanes = laneRepository.getAll();
        return TaskboardConfigToLaneConfigurationTransformer.getInstance().transform(lanes);
    }
    
    @Cacheable(CacheConfiguration.CONFIGURATION_COLOR)
    public Map<Pair<Long, Long>, String> getColorByIssueTypeAndStatus() {
        Map<Pair<Long, Long>, String> colorByIssueTypeAndStatus = new LinkedHashMap<>();
        laneConfiguration().stream()
            .flatMap(l->l.getStages().stream())
            .flatMap(s->s.getSteps().stream())
            .forEach(s->s.getIssuesConfiguration().stream().
                    forEach(ic->colorByIssueTypeAndStatus.put(Pair.of(ic.getIssueType(), ic.getStatus()), s.getColor()))
                    );

        return colorByIssueTypeAndStatus;
    }

}
