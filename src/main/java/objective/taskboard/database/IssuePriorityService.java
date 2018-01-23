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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.data.TaskboardIssue;
import objective.taskboard.issueBuffer.IssueBufferService;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.repository.TaskboardIssueRepository;

@Service
public class IssuePriorityService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IssuePriorityService.class);
    @Autowired
    TaskboardIssueRepository repo;
    
    @Autowired
    IssueBufferService issueBufferService;
    
    Map<String, TaskboardIssue> cache = new LinkedHashMap<>();
    
    @PostConstruct
    public void loadCache() {
        log.debug("Loading IssuePriorityService");
        cache.clear();
        List<TaskboardIssue> all = repo.findAll();
        for (TaskboardIssue taskboardIssue : all) 
            cache.put(taskboardIssue.getProjectKey(), taskboardIssue);
        log.debug("DONE Loading IssuePriorityService");
    }
    
    public Long determinePriority(JiraIssueDto jiraIssue) {
        TaskboardIssue priorityOrder = cache.get(jiraIssue.getKey());
        if (priorityOrder == null)
            return jiraIssue.getCreationDate().getMillis();

        return priorityOrder.getPriority();
    }

    public Optional<Date> priorityUpdateDate(JiraIssueDto jiraIssue) {
        TaskboardIssue priorityOrder = cache.get(jiraIssue.getKey());
        if (priorityOrder == null)
            return Optional.empty();
        return Optional.of(priorityOrder.getUpdated());
    }

    public synchronized List<TaskboardIssue> reorder(String[] issueKeys) {
        List<TaskboardIssue> issues = reorderAndSave(issueKeys);
        
        for (TaskboardIssue taskboardIssue : issues) 
            cache.put(taskboardIssue.getProjectKey(), taskboardIssue);
        
        return issues;
    }
    
    @Transactional
    private List<TaskboardIssue> reorderAndSave(String[] issueKeys) {
        List<TaskboardIssue> issues =new ArrayList<>(repo.findByIssueKeyIn(Arrays.asList(issueKeys)));
        List<TaskboardIssue> updatedIssues = new ArrayList<>(); 
        Map<String, TaskboardIssue> issueByKey = issues.stream().collect(Collectors.toMap(TaskboardIssue::getProjectKey, Function.identity()));
        
        for (String ti : issueKeys) {
            if (issueByKey.get(ti) == null){
                TaskboardIssue taskboardIssue = new TaskboardIssue(ti, issueBufferService.getIssueByKey(ti).getCreated());
                issueByKey.put(ti, taskboardIssue);
                issues.add(taskboardIssue);
            }
        }
        
        LinkedList<Long> priorities = new LinkedList<Long>(issues.stream().map(e -> e.getPriority()).collect(Collectors.toList()));
        Collections.sort(priorities);

        fixClashingPriorities(priorities);

        for (String pkey : issueKeys) {
            TaskboardIssue taskboardIssue = issueByKey.get(pkey);
            Long previousPriority = taskboardIssue.getPriority();
            Long newPriority = priorities.poll();
            if (previousPriority.equals(newPriority))
                continue;
            taskboardIssue.setPriority(newPriority);
            updatedIssues.add(repo.save(taskboardIssue));
        }
        return updatedIssues;
    }

    private void fixClashingPriorities(LinkedList<Long> priorities) {
        boolean hasRepeatedPriority = priorities.stream().anyMatch(p -> Collections.frequency(priorities, p) > 1);
        if (!hasRepeatedPriority)
            return;

        Long previous = priorities.get(0);
        for (int i = 1; i < priorities.size(); i++) {
            Long current = priorities.get(i);
            if (current <= previous) {
                current = previous + 1;
                priorities.set(i, current);
            }
            previous = current;
        }
    }

    public synchronized void reset() {
        loadCache();
    }
}
