package objective.taskboard.followup.impl;

import static java.util.Arrays.asList;
import static objective.taskboard.utils.DateTimeUtils.parseDate;
import static objective.taskboard.utils.DateTimeUtils.parseDateTime;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Status;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import objective.taskboard.data.Changelog;
import objective.taskboard.data.Issue;
import objective.taskboard.data.IssueScratch;
import objective.taskboard.data.TaskboardTimeTracking;
import objective.taskboard.issueBuffer.AllIssuesBufferService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.BallparkMapping;
import objective.taskboard.jira.JiraProperties.CustomField;
import objective.taskboard.jira.JiraProperties.CustomField.CustomFieldDetails;
import objective.taskboard.jira.JiraProperties.CustomField.TShirtSize;
import objective.taskboard.jira.JiraProperties.IssueLink;
import objective.taskboard.jira.JiraProperties.IssueType.IssueTypeDetails;
import objective.taskboard.jira.MetadataService;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractFollowUpDataProviderTest {

    @Mock
    protected JiraProperties jiraProperties;

    @Spy
    protected MetadataService metadataService;

    @Mock
    protected AllIssuesBufferService issueBufferService;

    CustomField propertiesCustomField;
    protected TShirtSize tshirtSizeInfo;

    protected JiraProperties.Followup followup = new JiraProperties.Followup();

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

    @Before
    public void before() throws InterruptedException, ExecutionException {
        Map<Long, Status> statusMap = new LinkedHashMap<>();
        statusMap.put(statusOpen,       new Status(null, statusOpen,       "Open", null, null));
        statusMap.put(statusToDo,       new Status(null, statusToDo,       "To Do", null, null));
        statusMap.put(statusDoing,      new Status(null, statusDoing,      "Doing", null, null));
        statusMap.put(statusCancelled,  new Status(null, statusCancelled,  "Cancelled", null, null));
        statusMap.put(statusDone,       new Status(null, statusDone,       "Done", null, null));
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
                "Alpha Testing", "To Alpha Test", "Doing", "To Do", "To Do", "Open" };
        JiraProperties.StatusPriorityOrder statusOrder = new JiraProperties.StatusPriorityOrder();
        statusOrder.setDemands(demandsOrder);
        statusOrder.setTasks(tasksOrder);
        statusOrder.setSubtasks(subtaskOrder);
        when(jiraProperties.getStatusPriorityOrder()).thenReturn(statusOrder);
    }

    public String[] defaultProjects() {
        return new String[]{"PROJ"};
    }

    public void configureBallparkMappings(String string) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<Long, List<BallparkMapping>> ballparkMappings;
        try {
            ballparkMappings = mapper.readValue(string,new TypeReference<Map<Long, List<BallparkMapping>>>(){});
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        followup.setBallparkMappings(ballparkMappings);
    }

    public class IssueBuilder {
        private long issueType;
        private Long id;
        private String project;
        private String key;
        private String summary;
        private Long status = statusToDo;
        private Integer originalEstimateMinutes;
        private Integer timeSpentMinutes;
        private String parent;
        private Map<String, Serializable> customFields = new LinkedHashMap<>();
        private Long priorityOrder;
        private List<Pair<String, ZonedDateTime>> transitions = new LinkedList<>();
        private Long created = 0L;

        public IssueBuilder id(int id) {
            this.id = (long) id;
            return this;
        }

        public IssueBuilder project(String p) {
            this.project=p;
            return this;
        }

        public IssueBuilder release(String releaseName) {
            String releaseId = jiraProperties.getCustomfield().getRelease().getId();
            customFields.put(releaseId, new objective.taskboard.data.CustomField(releaseId, releaseName));
            return this;
        }

        public IssueBuilder issueStatus(long status) {
            this.status = status;
            return this;
        }

        public IssueBuilder tshirt(String tshirtId, String tshirtSize) {
            customFields.put(tshirtId, new objective.taskboard.data.CustomField(tshirtId, tshirtSize));
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
                    0L, //startDateStepMillis
                    null, //subresponsavel1
                    null, //subresponsavel2
                    parent,
                    0L,   //parentType
                    null, //parentTypeIconUri
                    new ArrayList<String>(),//dependencies
                    null, //subResponsaveis
                    null, //assignee
                    0L, //priority
                    null, //dueDate
                    created, //created
                    null,//Date updatedDate,
                    null, //description
                    null, //comments
                    null, //labels
                    null, //components
                    customFields,
                    priorityOrder,
                    timeTracking,
                    null,//reporter
                    null,//coAssignees
                    null,//classOfService
                    null, //release
                    buildTransitions()
                    );
            return new Issue(scratch, jiraProperties, metadataService);
        }

        private List<Changelog> buildTransitions() {
            String currentState = "Open";
            List<Changelog> changes = new LinkedList<>();
            for(Pair<String, ZonedDateTime> t : transitions) {
                String newState = t.getLeft();
                ZonedDateTime timestamp = t.getRight();
                changes.add(new Changelog(null, "status", currentState, newState, timestamp));
                currentState = t.getKey();
            }
            return changes;
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

}