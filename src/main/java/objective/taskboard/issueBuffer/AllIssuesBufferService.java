package objective.taskboard.issueBuffer;

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

import static com.google.common.collect.Maps.newHashMap;
import static java.util.stream.Collectors.toList;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import objective.taskboard.data.Issue;
import objective.taskboard.domain.converter.IssueMetadata;
import objective.taskboard.domain.converter.JiraIssueToIssueConverter;
import objective.taskboard.jira.JiraIssueService;

@Slf4j
@Service
public class AllIssuesBufferService {
    @Autowired
    private JiraIssueToIssueConverter issueConverter;
    
    @Autowired
    private JiraIssueService jiraIssueService;
    
    private Map<String, IssueMetadata> allMetadatasByIssueKey = newHashMap();
    
    private Map<String, Issue> allIssuesBuffer = new LinkedHashMap<>();
    
    private boolean isUpdatingAllIssuesBuffer = false;
    
    private IssueBufferState state = IssueBufferState.uninitialised;
    
    public IssueBufferState getState() {
        return state;
    }
    
    public synchronized void updateAllIssuesBuffer() {
        if (isUpdatingAllIssuesBuffer)
            return;
        
        isUpdatingAllIssuesBuffer = true;
        Thread thread = new Thread(() -> {
            try {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                state = state.start();
                log.debug("updateAllIssuesBuffer start");
                allIssuesBuffer.clear();
                List<Issue> list = issueConverter.convertIssues(jiraIssueService.searchAllProjectIssues(), allMetadatasByIssueKey);
                
                Iterator<Issue> it = list.iterator();
                while (it.hasNext()) {
                    Issue issue = it.next();
                    allIssuesBuffer.put(issue.getIssueKey(), issue);
                    it.remove();
                }
                log.debug("All issues count: " + allIssuesBuffer.size());
                log.debug("updateAllIssuesBuffer complete");
                log.debug("updateAllIssuesBuffer time spent " +stopWatch.getTime());
                state = state.done();
            }
            catch (Exception e) {
                state = state.error();
                throw e;
            }
            finally {
                isUpdatingAllIssuesBuffer = false;
            }
        });
        thread.setName("AllIssuesBufferService.updateIssueBuffer()");
        thread.setDaemon(true);
        thread.start();
    }
    
    public synchronized List<Issue> getAllIssues() {
        return allIssuesBuffer.values().stream()
                .collect(toList());
    }
}
