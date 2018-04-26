package objective.taskboard.repository;

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

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import objective.taskboard.config.CacheConfiguration;
import objective.taskboard.domain.ProjectFilterConfiguration;

@Service
public class ProjectFilterConfigurationCachedRepository {

    private static final Logger log = LoggerFactory.getLogger(ProjectFilterConfigurationCachedRepository.class);

    @Autowired
    private ProjectFilterConfigurationRepository projectFilterRepository;

    private List<ProjectFilterConfiguration> cache = Lists.newArrayList();

    @Autowired
    private CacheManager cacheManager;

    @PostConstruct
    private synchronized void load() {
        loadCache();
    }

    public synchronized List<ProjectFilterConfiguration> getProjects() {
        return ImmutableList.copyOf(cache);
    }

    public synchronized Optional<ProjectFilterConfiguration> getProjectByKey(String projectKey) {
        return cache.stream().filter(p -> p.getProjectKey().equals(projectKey)).findFirst();
    }
    
    public synchronized ProjectFilterConfiguration getProjectByKeyOrCry(String projectKey) {
        return getProjectByKey(projectKey)
                .orElseThrow(() -> new IllegalArgumentException("Project with key '" + projectKey + "' not found"));
    }

    public synchronized boolean exists(String projectKey) {
        return cache.stream().anyMatch(p -> p.getProjectKey().equals(projectKey));
    }

    public synchronized void loadCache() {
        log.info("------------------------------ > ProjectFilterConfigurationRepository.loadCache()");
        cacheManager.getCache(CacheConfiguration.DASHBOARD_PROGRESS_DATA).clear();
        cacheManager.getCache(CacheConfiguration.USER_PROJECTS).clear();
        this.cache = projectFilterRepository.findAll();
    }

    public synchronized void save(ProjectFilterConfiguration f) {
        projectFilterRepository.save(f);
        loadCache();
    }

}
