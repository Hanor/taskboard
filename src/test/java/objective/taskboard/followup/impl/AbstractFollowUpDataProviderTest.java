package objective.taskboard.followup.impl;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static objective.taskboard.utils.DateTimeUtils.parseDate;
import static objective.taskboard.utils.DateTimeUtils.parseDateTime;
import static objective.taskboard.utils.DateTimeUtils.parseStringToDate;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import objective.taskboard.cycletime.CycleTime;
import objective.taskboard.data.Changelog;
import objective.taskboard.data.Issue;
import objective.taskboard.data.IssueScratch;
import objective.taskboard.data.TaskboardTimeTracking;
import objective.taskboard.data.Worklog;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.domain.converter.IssueCoAssignee;
import objective.taskboard.domain.converter.IssueTeamService;
import objective.taskboard.followup.EmptyFollowupCluster;
import objective.taskboard.followup.FollowupCluster;
import objective.taskboard.followup.FollowupClusterProvider;
import objective.taskboard.followup.FromJiraDataRow;
import objective.taskboard.followup.cluster.FollowUpClusterItem;
import objective.taskboard.issueBuffer.IssueBufferService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.BallparkMapping;
import objective.taskboard.jira.JiraProperties.CustomField;
import objective.taskboard.jira.JiraProperties.CustomField.Blocked;
import objective.taskboard.jira.JiraProperties.CustomField.CustomFieldDetails;
import objective.taskboard.jira.JiraProperties.CustomField.TShirtSize;
import objective.taskboard.jira.JiraProperties.IssueLink;
import objective.taskboard.jira.JiraProperties.IssueType.IssueTypeDetails;
import objective.taskboard.jira.MetadataService;
import objective.taskboard.jira.ProjectService;
import objective.taskboard.jira.data.Status;
import objective.taskboard.jira.data.StatusCategory;
import objective.taskboard.jira.data.Version;
import objective.taskboard.utils.Clock;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractFollowUpDataProviderTest {

    @Mock
    protected JiraProperties jiraProperties;

    @Spy
    protected MetadataService metadataService;

    @Mock
    protected IssueBufferService issueBufferService;

    @Mock
    private Clock localDateTimeService;

    @Mock
    private IssueTeamService issueTeamService;

    @Mock
    private CycleTime cycleTime;

    @Mock
    private ProjectService projectService;
    
    @Mock
    private FollowupClusterProvider clusterProvider;

    protected CustomField propertiesCustomField;
    protected TShirtSize tshirtSizeInfo;

    protected JiraProperties.Followup followup = new JiraProperties.Followup();
    protected ProjectFilterConfiguration projectConfiguration = new ProjectFilterConfiguration();

    @InjectMocks
    FollowUpDataProviderFromCurrentState subject;

    protected static final long demandIssueType   = 13;
    protected static final long taskIssueType     = 12;
    protected static final long devIssueType      = 14;
    protected static final long alphaIssueType    = 15;
    protected static final long reviewIssueType   = 16;
    protected static final long deployIssueType   = 17;
    protected static final long frontEndIssueType = 18;

    protected static final long statusOpen      = 11L;
    protected static final long statusToDo      = 13L;
    protected static final long statusDoing     = 15L;
    protected static final long statusCancelled = 16L;
    protected static final long statusDone      = 17L;
    protected static final long statusAlphaTesting      = 18L;
    protected static final long statusFeatureReviewing  = 19L;
    protected static final long statusQAing             = 20L;
    protected static final long statusToAlphaTest       = 21L;
    protected static final long statusToFeatureReview   = 22L;
    protected static final long statusToQA              = 23L;

    private static final String BLOCKED_ID = "1";
    private static final String LAST_BLOCK_REASON_ID = "2";
    private static final String ADDITIONAL_ESTIMATED_HOURS_ID = "3";

    protected static final StatusCategory CATEGORY_UNDEFINED = new StatusCategory(
            Long.valueOf(1L)
            , "undefined"
            , "medium-gray"
            , "No Category");
    protected static final StatusCategory CATEGORY_NEW = new StatusCategory(
            2L
            , "new"
            , "blue-gray"
            , "To Do");
    protected static final StatusCategory CATEGORY_IN_PROGRESS = new StatusCategory(
            4L
            , "indeterminate"
            , "yellow"
            , "In Progress");
    protected static final StatusCategory CATEGORY_DONE = new StatusCategory(
            3L
            , "done"
            , "green"
            , "Done");

    @Before
    public void before() throws InterruptedException, ExecutionException {
        when(clusterProvider.getForProject(any())).thenReturn(new EmptyFollowupCluster());

        Map<Long, Status> statusMap = new LinkedHashMap<>();
        statusMap.put(statusOpen,       new Status(statusOpen,       "Open",  CATEGORY_UNDEFINED));
        statusMap.put(statusToDo,       new Status(statusToDo,       "To Do", CATEGORY_UNDEFINED));
        statusMap.put(statusDoing,      new Status(statusDoing,      "Doing", CATEGORY_UNDEFINED));
        statusMap.put(statusCancelled,  new Status(statusCancelled,  "Cancelled", CATEGORY_UNDEFINED));
        statusMap.put(statusDone,       new Status(statusDone,       "Done",  CATEGORY_UNDEFINED));
        statusMap.put(statusAlphaTesting,      new Status(statusAlphaTesting,      "Alpha Testing",      CATEGORY_UNDEFINED));
        statusMap.put(statusFeatureReviewing,  new Status(statusFeatureReviewing,  "Feature Reviewing",  CATEGORY_UNDEFINED));
        statusMap.put(statusQAing,             new Status(statusQAing,             "QAing",              CATEGORY_UNDEFINED));
        statusMap.put(statusToAlphaTest,       new Status(statusToAlphaTest,       "To Alpha Test",      CATEGORY_UNDEFINED));
        statusMap.put(statusToFeatureReview,   new Status(statusToFeatureReview,   "To Feature Review",  CATEGORY_UNDEFINED));
        statusMap.put(statusToQA,              new Status(statusToQA,              "To QA",              CATEGORY_UNDEFINED));

        doReturn(statusMap).when(metadataService).getStatusesMetadata();

        Map<Long, IssueType> issueTypeMap = new LinkedHashMap<>();
        issueTypeMap.put(demandIssueType, new IssueType(null, demandIssueType, "Demand", false, null,null));
        issueTypeMap.put(taskIssueType,   new IssueType(null, taskIssueType,   "Task", false, null,null));
        issueTypeMap.put(devIssueType,    new IssueType(null, devIssueType,    "Dev", false, null,null));
        issueTypeMap.put(alphaIssueType,  new IssueType(null, alphaIssueType,  "Alpha", false, null,null));
        doReturn(issueTypeMap).when(metadataService).getIssueTypeMetadata();

        // tshirt size information
        tshirtSizeInfo = new TShirtSize();
        tshirtSizeInfo.setMainTShirtSizeFieldId("MAINID");
        propertiesCustomField = new CustomField();
        propertiesCustomField.setTShirtSize(tshirtSizeInfo);

        Blocked blocked = new Blocked();
        blocked.setId(BLOCKED_ID);
        propertiesCustomField.setBlocked(blocked);

        CustomFieldDetails additionalEstimatedHours = new CustomFieldDetails();
        additionalEstimatedHours.setId(ADDITIONAL_ESTIMATED_HOURS_ID);
        propertiesCustomField.setAdditionalEstimatedHours(additionalEstimatedHours);

        CustomFieldDetails lastBlockReason = new CustomFieldDetails();
        lastBlockReason.setId(LAST_BLOCK_REASON_ID);
        propertiesCustomField.setLastBlockReason(lastBlockReason);

        when(jiraProperties.getCustomfield()).thenReturn(propertiesCustomField);

        IssueLink issueLink = new IssueLink();
        when(jiraProperties.getIssuelink()).thenReturn(issueLink);

        JiraProperties.IssueType issueType = new JiraProperties.IssueType();
        issueType.setFeatures(asList(new IssueTypeDetails(taskIssueType)));
        issueType.setDemand(new IssueTypeDetails(demandIssueType));
        when(jiraProperties.getIssuetype()).thenReturn(issueType);

        followup.setBallparkDefaultStatus(statusOpen);
        when(jiraProperties.getFollowup()).thenReturn(followup);
        propertiesCustomField.setRelease(new CustomFieldDetails("RELEASE_CF_ID"));

        String[] demandsOrder = new String[] { "Cancelled", "Done", "UATing", "To UAT", "Doing", "To Do", "Open" };
        String[] subtaskOrder = new String[] { "Cancelled", "Done", "Reviewing", "To Review", "Doing", "To Do", "Open" };
        String[] tasksOrder = new String[] { "Cancelled", "Done", "QAing", "To QA", "Feature Reviewing", "To Feature Review",
                "Alpha Testing", "To Alpha Test", "Doing", "To Do", "Open" };
        JiraProperties.StatusPriorityOrder statusOrder = new JiraProperties.StatusPriorityOrder();
        statusOrder.setDemands(demandsOrder);
        statusOrder.setTasks(tasksOrder);
        statusOrder.setSubtasks(subtaskOrder);
        when(jiraProperties.getStatusPriorityOrder()).thenReturn(statusOrder);

        when(jiraProperties.getStatusesCompletedIds()).thenReturn(asList(10001L));
        when(jiraProperties.getStatusesCanceledIds()).thenReturn(asList(10101L));
        when(jiraProperties.getStatusesDeferredIds()).thenReturn(asList(10102L));

        when(cycleTime.getCycleTime(any(), any(), anyLong())).thenReturn(1D);
        
        projectConfiguration.setProjectKey("PROJ");
    }

    public String[] defaultProjects() {
        return new String[]{"PROJ"};
    }

    public void configureBallparkMappings(String... string) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<Long, List<BallparkMapping>> ballparkMappings;
        try {
            ballparkMappings = mapper.readValue(StringUtils.join(string, "\n"), new TypeReference<Map<Long, List<BallparkMapping>>>(){});
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        followup.setBallparkMappings(ballparkMappings);
    }

    protected void configureCluster(FollowUpClusterItem... items) {
        FollowupCluster cluster = mock(FollowupCluster.class);
        when(cluster.getClusterFor(any(), any())).thenReturn(Optional.empty());
        
        when(clusterProvider.getForProject(any())).thenReturn(cluster);

        for (FollowUpClusterItem item : items)
            when(cluster.getClusterFor(item.getSubtaskTypeName(), item.getSizing())).thenReturn(Optional.of(item));            
    }
    
    public class IssueBuilder {
        private long issueType;
        private Long id;
        private String project;
        private String key;
        private String summary;
        private String assignee;
        private List<IssueCoAssignee> coAssignees;
        private Long status = statusToDo;
        private long startDateStepMillis;
        private Integer originalEstimateMinutes;
        private Integer timeSpentMinutes;
        private Date priorityUpdatedDate;
        private String parent;
        private Map<String, Serializable> customFields = new LinkedHashMap<>();
        private Long priorityOrder = 0L;
        private List<Pair<String, ZonedDateTime>> transitions = new LinkedList<>();
        private Long created = 0L;
        private Date dueDate;
        private String releaseId;
        private List<String> labels;
        private String reporter;
        private List<String> components;
        private List<Worklog> worklogs = new LinkedList<>();

        public IssueBuilder id(int id) {
            this.id = (long) id;
            return this;
        }

        public IssueBuilder project(String p) {
            this.project=p;
            return this;
        }

        public IssueBuilder release(String releaseName) {
            when(projectService.getVersion(releaseName)).thenReturn(new Version(releaseName, releaseName));
            releaseId = releaseName;
            return this;
        }

        public IssueBuilder issueStatus(long status) {
            this.status = status;
            return this;
        }

        public IssueBuilder startDateStepMillis(long startDateStepMillis) {
            this.startDateStepMillis = startDateStepMillis;
            return this;
        }

        public IssueBuilder tshirt(String tshirtId, String tshirtSize) {
            customFields.put(tshirtId, new objective.taskboard.data.CustomField(tshirtId, tshirtSize));
            return this;
        }

        public IssueBuilder isBlocked(String yesNoValue) {
            customFields.put(BLOCKED_ID, yesNoValue);
            return this;
        }

        public IssueBuilder lastBlockReason(String lastBlockReason) {
            customFields.put(LAST_BLOCK_REASON_ID, lastBlockReason);
            return this;
        }

        public IssueBuilder additionalEstimatedHours(Double additionalEstimatedHours) {
            customFields.put(ADDITIONAL_ESTIMATED_HOURS_ID, new objective.taskboard.data.CustomField(ADDITIONAL_ESTIMATED_HOURS_ID, additionalEstimatedHours));
            return this;
        }

        public IssueBuilder parent(String parent) {
            this.parent = parent;
            return this;
        }

        public IssueBuilder issueType(long issueType) {
            this.issueType = issueType;
            return this;
        }

        public IssueBuilder timeSpentInHours(Integer hours) {
            this.timeSpentMinutes = hours * 60;
            return this;
        }

        public IssueBuilder originalEstimateInHours(Integer hours) {
            this.originalEstimateMinutes = (hours == null?0:hours) * 60;
            return this;
        }
        public IssueBuilder key(String key) {
            this.key = key;
            return this;
        }

        public IssueBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public IssueBuilder assignee(String assignee) {
            this.assignee = assignee;
            return this;
        }

        public IssueBuilder coAssignees(String... coAssigneesNames) {
            if (coAssigneesNames != null && coAssigneesNames.length > 0) {
                List<IssueCoAssignee> coAssignees = new ArrayList<>();
                for (int i = 0 ; i < coAssigneesNames.length; i++)
                    coAssignees.add(new IssueCoAssignee(coAssigneesNames[i], "avatarUrl-" + coAssigneesNames[i]));
                this.coAssignees = coAssignees;
            }
            return this;
        }

        public IssueBuilder reporter(String reporter) {
            this.reporter = reporter;
            return this;
        }

        public IssueBuilder priorityOrder(Long priorityOrder) {
            this.priorityOrder = priorityOrder;
            return this;
        }

        public IssueBuilder tshirtSize(String tShirtSize) {
            return tshirt(tshirtSizeInfo.getMainTShirtSizeFieldId(), tShirtSize);
        }

        public IssueBuilder transition(String status, String date) {
            return transition(status, parseDate(date));
        }

        public IssueBuilder transition(String status, ZonedDateTime date) {
            this.transitions.add(Pair.of(status, date));
            return this;
        }

        public IssueBuilder transition(String status, String date, String time) {
            return transition(status, parseDateTime(date, time));
        }

        public IssueBuilder created(Long created) {
            this.created = created;
            return this;
        }

        public IssueBuilder created(ZonedDateTime created) {
            return created(created.toInstant().toEpochMilli());
        }
        
        public IssueBuilder created(String created) {
            return created(parseDate(created));
        }

        public IssueBuilder created(String date, String time) {
            return created(parseDateTime(date, time));
        }

        public IssueBuilder dueDate(String dueDate) {
            this.dueDate = parseStringToDate(dueDate);
            return this;
        }

        public IssueBuilder priorityUpdatedDate(String priorityUpdatedDate) {
            this.priorityUpdatedDate = parseStringToDate(priorityUpdatedDate);
            return this;
        }

        public IssueBuilder labels(String... labels) {
            if (labels != null && labels.length > 0)
                this.labels = new ArrayList<String>(asList(labels));
            return this;
        }

        public IssueBuilder components(String... components) {
            if (components != null && components.length > 0)
                this.components = new ArrayList<String>(asList(components));
            return this;
        }

        public Issue build() {
            TaskboardTimeTracking timeTracking = new TaskboardTimeTracking(originalEstimateMinutes, timeSpentMinutes);
            if (originalEstimateMinutes == null && timeSpentMinutes == null)
                timeTracking = null;
            IssueScratch scratch = new IssueScratch(
                    id,
                    key,
                    getProjectKey(project),
                    getProjectName(),
                    issueType,
                    null, //typeIconUri
                    summary,
                    status,
                    startDateStepMillis, //startDateStepMillis
                    null, //subresponsavel1
                    null, //subresponsavel2
                    parent,
                    0L,   //parentType
                    null, //parentTypeIconUri
                    new ArrayList<String>(),//dependencies
                    null, //subResponsaveis
                    assignee,
                    0L, //priority
                    dueDate,
                    created,
                    priorityUpdatedDate,
                    null,//Date remoteUpdatedDate,
                    null, //description
                    null, //comments
                    labels,
                    components,
                    customFields,
                    priorityOrder,
                    timeTracking,
                    reporter,
                    coAssignees,
                    null,//classOfService
                    releaseId,
                    buildTransitions(),
                    worklogs
                    );
            return new Issue(scratch, jiraProperties, metadataService, issueTeamService, null, cycleTime, null, projectService, null, null);
        }

        private List<Changelog> buildTransitions() {
            String currentState = "Open";
            List<Changelog> changes = new LinkedList<>();
            for(Pair<String, ZonedDateTime> t : transitions) {
                String newState = t.getLeft();
                ZonedDateTime timestamp = t.getRight();
                changes.add(new Changelog(null, "status", currentState, newState, "42",timestamp));
                currentState = t.getKey();
            }
            return changes;
        }

        public IssueBuilder worklog(String author, String startedStr, int timeSpentSeconds) {
            worklogs.add(new Worklog(author, parseStringToDate(startedStr), timeSpentSeconds));
            return this;
        }
    }

    public void issues(IssueBuilder ... builders) {
        List<Issue> issueList = new ArrayList<>();
        for(IssueBuilder b : builders)
            issueList.add(b.build());
        when(issueBufferService.getAllIssues()).thenReturn(issueList);
    }

    public IssueBuilder demand() {
        return new IssueBuilder()
                .issueType(demandIssueType);
    }

    public IssueBuilder subtask() {
        return new IssueBuilder();
    }

    public IssueBuilder task() {
        return new IssueBuilder().issueType(taskIssueType);
    }

    private static String getProjectName() {
        return "A Project";
    }

    private static String getProjectKey(String project) {
        if (project != null)
            return project;
        return "PROJ";
    }
    
    protected void assertFromJiraRows(Function<FromJiraDataRow, List<Object>> fieldsExtractor, String... expectedRows) {
        List<FromJiraDataRow> actualRows = subject.getJiraData(defaultProjects(), ZoneId.systemDefault()).getData().fromJiraDs.rows;
        
        String actualString = actualRows.stream()
                .map(fieldsExtractor)
                .map(fields -> fields.stream().map(f -> f == null ? "<null>" : f.toString().trim()).collect(joining(" | ")))
                .collect(joining("\n"));
        
        String expectedString = Stream.of(expectedRows)
                .map(r -> Stream.of(r.split("\\|")).map(f -> f.trim()).collect(joining(" | ")))
                .collect(joining("\n"));
        
        assertEquals(expectedString, actualString);
    }
}