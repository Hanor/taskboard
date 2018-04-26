package objective.taskboard.issueBuffer;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import objective.taskboard.data.IssueScratch;
import objective.taskboard.domain.converter.IncompleteIssueException;
import objective.taskboard.domain.converter.JiraIssueToIssueConverter;
import objective.taskboard.domain.converter.ParentProvider;
import objective.taskboard.jira.SearchIssueVisitor;
import objective.taskboard.jira.client.JiraIssueDto;

public class IssueBufferServiceSearchVisitor implements SearchIssueVisitor {
    private static final Logger log = LoggerFactory.getLogger(IssueBufferService.class);
    
    private final JiraIssueToIssueConverter issueConverter;
    private final IssueBufferService issueBufferService;
    private final Map<String, List<IssueScratch>> pending = new LinkedHashMap<>();

    private final ParentProvider provider;
    private int processedCount = 0;
    
    public IssueBufferServiceSearchVisitor(JiraIssueToIssueConverter issueConverter, IssueBufferService issueBufferService) {
        this.issueConverter = issueConverter;
        this.issueBufferService = issueBufferService;

        provider = parentKey -> {
            return Optional.ofNullable(this.issueBufferService.getIssueByKey(parentKey));
        };
    }

    @Override
    public void processIssue(JiraIssueDto jiraIssue) {
        String currentIssueKey = jiraIssue.getKey();
        try {
            issueBufferService.updateIssue(issueConverter.convertSingleIssue(jiraIssue, provider));
            processedCount++;
        }catch(IncompleteIssueException e) {//NOSONAR - this is expected and correctly handled
            String parentKey = e.getMissingParentKey();
            List<IssueScratch> issuesDependingOnParent = pending.get(parentKey);
            if (issuesDependingOnParent == null) {
                issuesDependingOnParent = new ArrayList<>();
                pending.put(parentKey, issuesDependingOnParent);
            }
            issuesDependingOnParent.add(e.getIncompleteIssue());
            return;
        }
        
        if (!pending.containsKey(currentIssueKey)) 
            return;
        
        LinkedList<IssueScratch> issuesToConvert = new LinkedList<>();
        
        issuesToConvert.addAll(pending.remove(currentIssueKey));
        
        while (issuesToConvert.size() > 0) {
            IssueScratch scratch = issuesToConvert.poll();
            if (!issueBufferService.updateIssue(issueConverter.createIssueFromScratch(scratch, provider)))
                log.debug("Issue key " + scratch.getIssueKey() + " fetched, but current copy is newer. Ignoring.");
            
            processedCount++;
            issuesToConvert.addAll(defaultIfNull(pending.remove(scratch.getIssueKey()), emptyList()));
        }
    }

    @Override
    public void complete() {
        validateNotPending();
    }
    
    public int getProcessedCount() {
        return processedCount;
    }
    
    private void validateNotPending() {
        List<String> missingParents = new LinkedList<>();
        for (Entry<String, List<IssueScratch>> each : pending.entrySet()) 
            if (each.getValue().size() > 0) 
                missingParents.add(each.getKey());
        
        if (missingParents.size() > 0)
            throw new IllegalStateException("Some parents were never found: " + StringUtils.join(missingParents,","));
    }
}
