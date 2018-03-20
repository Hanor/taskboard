package objective.taskboard.config;

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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.cache.CacheBuilder;

@Configuration
@EnableCaching
public class CacheConfiguration {
    public static final String CONFIGURATION = "configuration";
    public static final String STATUSES_METADATA = "statusesMetadata";
    public static final String PRIORITIES_METADATA = "prioritiesMetadata";
    public static final String ISSUE_TYPE_METADATA = "issueTypeMetadata";
    public static final String CONFIGURED_TEAMS = "configured-teams";
    public static final String TEAMS_VISIBLE_TO_USER = "teams-visible-to-user";
    public static final String USER_PROJECTS = "user-projects";
    public static final String ALL_PROJECTS = "all-projects";
    public static final String JIRA_FIELD_METADATA = "jira-field-metadata";
    public static final String JIRA_FIELD_METADATA_LOCALIZED = "jira-field-metadata-localized";
    public static final String HOLIDAYS = "holidays";
    public static final String ISSUE_LINKS_METADATA = "issueLinksMetadata";
    public static final String JIRA_TIME_ZONE = "jiraTimeZone";
    public static final String DASHBOARD_PROGRESS_DATA = "dashboardProgressData";
    public static final String JIRA_USER = "jiraUser";
    public static final String COLOR_BY_ISSUETYPE_AND_STATUS = "colorByIssueTypeAndStatus";
    public static final String CONFIGURATION_COLOR = "getColorByIssueTypeAndStatusMap";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        simpleCacheManager.setCaches(Arrays.asList(
                new GuavaCache(CONFIGURATION, CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).concurrencyLevel(1).build()),
                new GuavaCache(ISSUE_TYPE_METADATA, CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).concurrencyLevel(1).build()),
                new GuavaCache(CONFIGURATION_COLOR, CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).concurrencyLevel(1).build()),
                new GuavaCache(COLOR_BY_ISSUETYPE_AND_STATUS, CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).concurrencyLevel(1).build()),
                new GuavaCache(ISSUE_TYPE_METADATA, CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).concurrencyLevel(1).build()),
                new GuavaCache(PRIORITIES_METADATA, CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).concurrencyLevel(1).build()),
                new GuavaCache(STATUSES_METADATA, CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).concurrencyLevel(1).build()),
                new GuavaCache(ISSUE_LINKS_METADATA, CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).concurrencyLevel(1).build()),
                new GuavaCache(CONFIGURED_TEAMS, CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).concurrencyLevel(1).build()),
                new GuavaCache(TEAMS_VISIBLE_TO_USER, CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).concurrencyLevel(1).build()),
                new GuavaCache(USER_PROJECTS, CacheBuilder.newBuilder().expireAfterWrite(6, TimeUnit.HOURS).concurrencyLevel(1).build()),
                new GuavaCache(ALL_PROJECTS, CacheBuilder.newBuilder().expireAfterWrite(6, TimeUnit.HOURS).concurrencyLevel(1).build()),
                new GuavaCache(JIRA_FIELD_METADATA, CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).concurrencyLevel(1).build()),
                new GuavaCache(JIRA_FIELD_METADATA_LOCALIZED, CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).concurrencyLevel(1).build()),
                new GuavaCache(HOLIDAYS, CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).concurrencyLevel(1).build()),
                new GuavaCache(JIRA_TIME_ZONE, CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).concurrencyLevel(1).build()),
                new GuavaCache(DASHBOARD_PROGRESS_DATA, CacheBuilder.newBuilder().maximumSize(20).expireAfterWrite(1, TimeUnit.DAYS).concurrencyLevel(1).build()),
                new GuavaCache(JIRA_USER, CacheBuilder.newBuilder().expireAfterWrite(6, TimeUnit.HOURS).concurrencyLevel(1).build())
        ));
        return simpleCacheManager;
    }

}
