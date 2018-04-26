package objective.taskboard.issueBuffer;

import java.io.File;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import objective.taskboard.cycletime.CycleTime;
import objective.taskboard.database.IssuePriorityService;
import objective.taskboard.database.directory.DataBaseDirectory;
import objective.taskboard.domain.IssueColorService;
import objective.taskboard.domain.converter.CardVisibilityEvalService;
import objective.taskboard.domain.converter.IssueTeamService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.MetadataService;
import objective.taskboard.jira.ProjectService;
import objective.taskboard.repository.FilterCachedRepository;

@Component
@Profile({"prod", "dev"})
public class CardRepoServiceImpl implements CardRepoService {
    private static final Logger log = LoggerFactory.getLogger(CardRepo.class);
    
    @Autowired
    private JiraProperties jiraProperties;
    
    @Autowired
    private MetadataService metaDataService;
    
    @Autowired
    private DataBaseDirectory dataDirectory;
    
    @Autowired
    private IssueTeamService issueTeamService;
    
    @Autowired
    private FilterCachedRepository filterRepository;

    @Autowired
    private CycleTime cycleTime;

    @Autowired
    private CardVisibilityEvalService cardVisibilityEvalService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private IssueColorService issueColorService;

    @Autowired
    private IssuePriorityService issuePriorityService;

    public CardRepo from(String cacheName) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        File cachefile = dataDirectory.path(cacheName).toFile();
        
        CardRepo repo = new CardRepo(new CardMapDB(cachefile));
        
        if (!cachefile.exists()) {
            log.warn("Cache not found. Nothing to load");
            return repo;
        }
        
        repo.values().stream().forEach(c-> {
            c.setParentCard(repo.get(c.getParent()));
            c.restoreServices(
                    jiraProperties,
                    metaDataService,
                    issueTeamService,
                    filterRepository,
                    cycleTime,
                    cardVisibilityEvalService,
                    projectService,
                    issueColorService,
                    issuePriorityService);
        });

        log.info(cachefile.getAbsolutePath()+ " data read in " + stopWatch.getTime() + " ms. Loaded " + repo.size() + " issues");        
        return repo;
    }
}
