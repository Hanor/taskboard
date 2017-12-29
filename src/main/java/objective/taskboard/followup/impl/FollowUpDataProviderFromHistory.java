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

import static objective.taskboard.issueBuffer.IssueBufferState.ready;

import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;

import objective.taskboard.followup.FollowUpDataEntry;
import objective.taskboard.followup.FollowUpDataHistoryRepository;
import objective.taskboard.followup.FollowupDataProvider;
import objective.taskboard.issueBuffer.IssueBufferState;

public class FollowUpDataProviderFromHistory implements FollowupDataProvider {

    private final String date;
    private final FollowUpDataHistoryRepository historyRepository;

    public FollowUpDataProviderFromHistory(String date, FollowUpDataHistoryRepository historyRepository) {
        this.date = date;
        this.historyRepository = historyRepository;
    }

    @Override
    public FollowUpDataEntry getJiraData(String[] includeProjects, ZoneId timezone) {
        return historyRepository.get(date, timezone, includeProjects);
    }

    @Override
    public IssueBufferState getFollowupState() {
        return ready;
    }

    @Override
    public void forEachHistoryEntry(
            List<String> projectsKey, 
            ZoneId timezone,
            Consumer<FollowUpDataEntry> action) {

        historyRepository.forEachHistoryEntry(projectsKey, date, timezone, action);
    }
}
