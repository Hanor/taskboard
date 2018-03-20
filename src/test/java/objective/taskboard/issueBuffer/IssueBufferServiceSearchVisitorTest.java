package objective.taskboard.issueBuffer;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import org.codehaus.jettison.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import objective.taskboard.data.Issue;
import objective.taskboard.database.IssuePriorityService;
import objective.taskboard.domain.IssueColorService;
import objective.taskboard.domain.converter.CardVisibilityEvalService;
import objective.taskboard.domain.converter.IssueTeamService;
import objective.taskboard.domain.converter.JiraIssueToIssueConverter;
import objective.taskboard.domain.converter.StartDateStepService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.CustomField;
import objective.taskboard.jira.JiraProperties.CustomField.Blocked;
import objective.taskboard.jira.JiraProperties.CustomField.CustomFieldDetails;
import objective.taskboard.jira.JiraProperties.CustomField.TShirtSize;
import objective.taskboard.jira.JiraProperties.IssueLink;
import objective.taskboard.jira.JiraProperties.IssueType;
import objective.taskboard.jira.JiraProperties.IssueType.IssueTypeDetails;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.jira.client.JiraIssueDtoSearch;
import objective.taskboard.jira.client.JiraSearchTestSupport;
import objective.taskboard.repository.FilterCachedRepository;
import objective.taskboard.utils.IOUtilities;

@RunWith(MockitoJUnitRunner.class)
public class IssueBufferServiceSearchVisitorTest {
    
    @Spy
    private JiraProperties properties = new JiraProperties();
    
    @Mock
    private IssuePriorityService priorityService;
    
    @Mock
    private FilterCachedRepository filterCachedRepo;
    
    @Mock
    private CardVisibilityEvalService cardVisibilityEvalService;
    
    @Mock
    private StartDateStepService startDateStepService;
    
    @Mock
    private IssueTeamService issueTeamService;
    
    @Mock
    private IssueColorService issueColorService;

    @Mock
    private IssueBufferService issueBufferService;

    @Mock
    private Issue issue;

    @InjectMocks
    JiraIssueToIssueConverter issueConverter ;
    
    @Before
    public void setup() {
        CustomField customFieldConfiguration = new CustomField();
        properties.setCustomfield(customFieldConfiguration);
        CustomFieldDetails coAssignees = new CustomFieldDetails();
        coAssignees.setId("customfield_11456");
        customFieldConfiguration.setCoAssignees(coAssignees);
        customFieldConfiguration.setBlocked(new Blocked());
        CustomFieldDetails lastBlockReason = new CustomFieldDetails();
        lastBlockReason.setId("customfield_11452");
        customFieldConfiguration.setLastBlockReason(lastBlockReason);
        CustomFieldDetails additionalEstimatedHours = new CustomFieldDetails();
        additionalEstimatedHours.setId("customfield_11450");
        customFieldConfiguration.setAdditionalEstimatedHours(additionalEstimatedHours);
        CustomFieldDetails release = new CustomFieldDetails();
        release.setId("customfield_11455");
        customFieldConfiguration.setRelease(release);
        TShirtSize tShirtSize = new TShirtSize();
        tShirtSize.setIds(new ArrayList<String>());
        customFieldConfiguration.setTShirtSize( tShirtSize);
        CustomFieldDetails assignedTeams = new CustomFieldDetails();
        release.setId("customfield_10100");
        customFieldConfiguration.setAssignedTeams(assignedTeams);
        
        
        IssueType issueType = new IssueType();
        issueType.setDemand(new IssueTypeDetails());
        properties.setIssuetype(issueType);
        
        when(priorityService.determinePriority(any())).thenReturn(1l);
        when(priorityService.priorityUpdateDate((any()))).thenReturn(new Date());
        
        when(issueColorService.getColor(any())).thenReturn("#FFFFFF");
        
        IssueLink issuelink = new IssueLink();
        issuelink.setDependencies(Arrays.asList());
        properties.setIssuelink(issuelink);
        
        when(cardVisibilityEvalService.calculateVisibleUntil(any(), any(), any())).thenReturn(Optional.empty());
    }
    
    @Test
	public void whenProcessingIssueWithoutParent() throws JSONException {
        IssueBufferServiceSearchVisitor subject = new IssueBufferServiceSearchVisitor(issueConverter, issueBufferService);
	    
        JiraSearchTestSupport searchResultParser = new JiraSearchTestSupport();
        JiraIssueDtoSearch searchResult = searchResultParser.parse(result("TASKB-685"));
        ArrayList<JiraIssueDto> list = newArrayList(searchResult.getIssues());
        
        list.stream().forEach(subject::processIssue);
        
        assertEquals(1, subject.getProcessedCount());
        subject.complete();
	}
    
    @Test
    public void whenIssuesAreReturnedOutOfOrder() throws JSONException {
        IssueBufferServiceSearchVisitor subject = new IssueBufferServiceSearchVisitor(issueConverter, issueBufferService);
        
        JiraSearchTestSupport searchResultParser = new JiraSearchTestSupport();
        JiraIssueDtoSearch searchResult = searchResultParser.parse(result("TASKB-686_TASKB-685"));
        ArrayList<JiraIssueDto> list = newArrayList(searchResult.getIssues());

        list.stream().forEach(jiraIssue -> {
            if ("TASKB-685".equals(jiraIssue.getKey())) {
                when(issueBufferService.getIssueByKey("TASKB-685")).thenReturn(issue);
                assertEquals(0, subject.getProcessedCount());
            }
            subject.processIssue(jiraIssue);
        });

        assertEquals(2, subject.getProcessedCount());
        subject.complete();
    }
    
    @Test
    public void whenIssuesAreReturnedOutOfOrderWithNestedDependencies() throws JSONException {
        when(issueBufferService.updateIssue(any())).thenReturn(true);
        issueConverter.setParentIssueLinks(Arrays.asList("is demanded by"));
        
        IssueBufferServiceSearchVisitor subject = new IssueBufferServiceSearchVisitor(issueConverter, issueBufferService);
        
        JiraSearchTestSupport searchResultParser = new JiraSearchTestSupport();
        JiraIssueDtoSearch searchResult = searchResultParser.parse(result("TASKB-634_TASKB-630_TASKB-628"));
        ArrayList<JiraIssueDto> list = newArrayList(searchResult.getIssues());

        list.stream().forEach(jiraIssue -> {
            if ("TASKB-628".equals(jiraIssue.getKey())) {
                when(issueBufferService.getIssueByKey("TASKB-628")).thenReturn(issue);
                when(issueBufferService.getIssueByKey("TASKB-630")).thenReturn(issue);
                assertEquals(0, subject.getProcessedCount());
            }
            subject.processIssue(jiraIssue);
        });

        assertEquals(3, subject.getProcessedCount());
        subject.complete();
    }
    
    @Test
    public void whenIssuesAreReturnedInOrder() throws JSONException {
        when(issueBufferService.getAllIssues()).thenReturn(newArrayList(issue));
        when(issueBufferService.getIssueByKey("TASKB-685")).thenReturn(issue);

        IssueBufferServiceSearchVisitor subject = new IssueBufferServiceSearchVisitor(issueConverter, issueBufferService);
        
        JiraSearchTestSupport searchResultParser = new JiraSearchTestSupport();
        JiraIssueDtoSearch searchResult = searchResultParser.parse(result("TASKB-685_TASKB-686"));
        ArrayList<JiraIssueDto> list = newArrayList(searchResult.getIssues());
        
        list.stream().forEach(subject::processIssue);
        
        assertEquals(2, subject.getProcessedCount());
        subject.complete();
    }

    @Test
    public void whenProcessingIssueWithMissingParent_ShouldThrowIllegalStateException() throws JSONException {
        IssueBufferServiceSearchVisitor subject = new IssueBufferServiceSearchVisitor(issueConverter, issueBufferService);

        JiraSearchTestSupport searchResultParser = new JiraSearchTestSupport();
        JiraIssueDtoSearch searchResult = searchResultParser.parse(result("TASKB-686"));
        ArrayList<JiraIssueDto> list = newArrayList(searchResult.getIssues());

        list.stream().forEach(subject::processIssue);

        assertEquals(0, subject.getProcessedCount());
        try {
            subject.complete();
            fail("Should throw an IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Some parents were never found: TASKB-685", e.getMessage());
        }
    }

    public static String result(String string) {
        return IOUtilities.resourceToString(IOUtilities.class, "/objective-jira-teste/"+"search_" + string + ".json");
    }
}