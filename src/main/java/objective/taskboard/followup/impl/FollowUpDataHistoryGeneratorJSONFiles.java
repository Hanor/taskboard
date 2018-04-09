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
package objective.taskboard.followup.impl;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.followup.FollowUpDataHistoryGenerator;
import objective.taskboard.followup.FollowUpDataHistoryRepository;
import objective.taskboard.followup.FollowUpDataSnapshot;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;

@Component
public class FollowUpDataHistoryGeneratorJSONFiles implements FollowUpDataHistoryGenerator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FollowUpDataHistoryGeneratorJSONFiles.class);

    private final ProjectFilterConfigurationCachedRepository projectFilterCacheRepo;
    private final FollowUpDataProviderFromCurrentState providerFromCurrentState;
    private final FollowUpDataHistoryRepository historyRepository;

    private boolean isExecutingDataHistoryGenerate = false;
    
    @Autowired
    public FollowUpDataHistoryGeneratorJSONFiles(
            ProjectFilterConfigurationCachedRepository projectFilterCacheRepo,
            FollowUpDataProviderFromCurrentState providerFromCurrentState,
            FollowUpDataHistoryRepository historyRepository) {
        this.projectFilterCacheRepo = projectFilterCacheRepo;
        this.providerFromCurrentState = providerFromCurrentState;
        this.historyRepository = historyRepository;
    }

    public void initialize() {
        syncProjectsHistoryOnDatabase();
        scheduledGenerate();
    }

    @Override
    public void scheduledGenerate() {
        if (isExecutingDataHistoryGenerate)
            return;

        isExecutingDataHistoryGenerate = true;
        Thread thread = new Thread(() -> {
            try {
                generate();
            } catch (IOException|InterruptedException e ) {
                throw new RuntimeException(e);
            } finally {
                isExecutingDataHistoryGenerate = false;
            }
        });
        thread.setName(getClass().getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }

    private void syncProjectsHistoryOnDatabase() {
        log.info(getClass().getSimpleName() + ".syncProjectsHistoryOnDatabase start");
        for (ProjectFilterConfiguration pf : projectFilterCacheRepo.getProjects()) {
            log.info("Synching history of project " + pf.getProjectKey());
            historyRepository.syncEffortHistory(pf.getProjectKey());
            log.info("Synching history of project " + pf.getProjectKey() + " complete");
        }
        log.info(getClass().getSimpleName() + ".syncProjectsHistoryOnDatabase complete");
    }

    protected void generate() throws IOException, InterruptedException {
        log.info(getClass().getSimpleName() + " start");
        for (ProjectFilterConfiguration pf : projectFilterCacheRepo.getProjects()) {
            String projectKey = pf.getProjectKey();

            log.info("Generating history of project " + projectKey);
            FollowUpDataSnapshot followUpDataEntry = providerFromCurrentState.getJiraData(projectKey);
            historyRepository.save(projectKey, followUpDataEntry);
            
            log.info("History of project " + projectKey + " generated");
        }
        log.info(getClass().getSimpleName() + " complete");
    }
}
