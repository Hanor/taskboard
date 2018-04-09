package objective.taskboard.followup.impl;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.atlassian.jira.rest.client.api.domain.IssueType;

import objective.taskboard.data.Issue;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.followup.FollowUpDataHistoryRepository;
import objective.taskboard.followup.FollowUpDataSnapshot;
import objective.taskboard.followup.FollowupCluster;
import objective.taskboard.followup.FollowupData;
import objective.taskboard.followup.FromJiraDataRow;
import objective.taskboard.followup.FromJiraDataSet;
import objective.taskboard.followup.FromJiraRowCalculator;
import objective.taskboard.followup.FromJiraRowCalculator.FromJiraRowCalculation;
import objective.taskboard.issueBuffer.IssueBufferService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.BallparkMapping;
import objective.taskboard.jira.MetadataService;
import objective.taskboard.jira.data.Status;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;

@Component
public class FollowUpDataMigrator implements CommandLineRunner {

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private FollowUpDataHistoryRepository historyRepository;
    
    @Autowired
    private IssueBufferService issueBufferService;
    
    @Autowired
    private ProjectFilterConfigurationCachedRepository projectFilterCacheRepo;
    
    private Map<String, Status> issueStatusByName;
    private Map<Long, Status> statusById;
    private Map<String, IssueType> issueTypesByName;
    private Map<Long, List<String>> bmIssueTypesByFeatureType;
    private Set<String> notFoundStatusNames;
    private Set<String> issuesWithChangedType;
    private Set<String> deletedIssues;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("&&&&&&&&&&&&&&&& Follow-up migration init...");
        
        setup();
        
        ZoneId timezone = ZoneId.systemDefault();

        for (ProjectFilterConfiguration project : projectFilterCacheRepo.getProjects()) {
            System.out.println("  " + project.getProjectKey());
            
            for (String followupDate : historyRepository.getHistoryGivenProjects(true, project.getProjectKey())) {
                System.out.println("    " + followupDate);
                new FileMigration(project.getProjectKey(), followupDate, timezone).execute();
            }
        }
        
        printReport();

        System.out.println("&&&&&&&&&&&&&&&& Migration done!");
    }

    private void printReport() {
        System.out.println("--------------------------------------------------------");
        
        if (!notFoundStatusNames.isEmpty())
            System.out.println("### Status not found:\n  " + notFoundStatusNames.stream().sorted().collect(joining("\n  ")) + "\n");
        
        if (!issuesWithChangedType.isEmpty())
            System.out.println("### Issues changed type:\n  " + issuesWithChangedType.stream().sorted().collect(joining("\n  ")) + "\n");
        
        if (!issuesWithChangedType.isEmpty())
            System.out.println("### Deleted issues:\n  " + deletedIssues.stream().sorted().collect(joining("\n  ")) + "\n");
        
        System.out.println("--------------------------------------------------------");
    }

    private void setup() {
        this.notFoundStatusNames = new HashSet<>();
        this.issuesWithChangedType = new HashSet<>();
        this.deletedIssues = new HashSet<>();
        
        this.statusById = metadataService.getStatusesMetadata();
        this.issueStatusByName = statusById.values().stream().collect(toMap(s -> s.name, s -> s));
        this.issueTypesByName = metadataService.getIssueTypes().stream().collect(toMap(t -> t.getName(), t -> t));
        
        this.bmIssueTypesByFeatureType = jiraProperties.getFollowup().getBallparkMappings().entrySet().stream()
                .collect(toMap(
                        e -> e.getKey(), 
                        e -> e.getValue().stream().map(BallparkMapping::getIssueType).collect(toList())));
    }

    private class FileMigration {
        private final String projectKey;
        private final String followupDate;
        private final ZoneId timezone;

        private FollowupCluster cluster;
        private FromJiraRowCalculator fromJiraRowCalculator;
        private Map<String, FromJiraDataRow> featureBallparks;

        private FileMigration(String projectKey, String followupDate, ZoneId timezone) {
            this.projectKey = projectKey;
            this.followupDate = followupDate;
            this.timezone = timezone;
        }

        public void execute() {
            FollowUpDataSnapshot originalSnapshot = historyRepository.get(followupDate, timezone, projectKey);
            List<FromJiraDataRow> rows = new ArrayList<>(originalSnapshot.getData().fromJiraDs.rows);
            List<FromJiraDataRow> subtaskRows = rows.stream().filter(r -> FromJiraDataRow.QUERY_TYPE_SUBTASK_PLAN.equals(r.queryType)).collect(toList());
            
            this.cluster = originalSnapshot.getCluster();
            this.fromJiraRowCalculator = new FromJiraRowCalculator(cluster);
            this.featureBallparks = rows.stream()
                    .filter(r -> FromJiraDataRow.QUERY_TYPE_FEATURE_BALLPARK.equals(r.queryType))
                    .collect(toLinkedMap(r -> featureBallparkKey(r.taskNum, r.subtaskType), r -> r));
            
            resetTaskBallparkValueOfSubtaskRows(rows);
            makeFeatureBallparks(subtaskRows);
            fillTaskBallparkValueOfFeatureBallparkRows();
            ensureMinimal(subtaskRows);
            syncFeatureBallparks(rows);
            sortRows(rows);

            overrideHistoryFile(originalSnapshot, rows);
        }

        private void sortRows(List<FromJiraDataRow> rows) {
            rows.sort(Comparator.comparingInt((FromJiraDataRow f) -> f.demandStatusPriority)
                    .thenComparingLong(f -> f.demandPriorityOrder)
                    .thenComparingInt(f -> f.taskStatusPriority)
                    .thenComparingLong(f -> f.taskPriorityOrder)
                    .thenComparingInt(f -> f.subtaskStatusPriority)
                    .thenComparingLong(f -> f.subtaskPriorityOrder)
                    .thenComparing((r1, r2) -> {
                        if (!r1.queryType.equals(FromJiraDataRow.QUERY_TYPE_FEATURE_BALLPARK) 
                                || !r2.queryType.equals(FromJiraDataRow.QUERY_TYPE_FEATURE_BALLPARK) 
                                || !r1.taskNum.equals(r2.taskNum)) {
                            return 0;
                        }

                        long featureTypeId = issueTypesByName.get(r1.taskType).getId();
                        List<String> bmIssueTypes = bmIssueTypesByFeatureType.get(featureTypeId);

                        return bmIssueTypes.indexOf(r1.subtaskType) - bmIssueTypes.indexOf(r2.subtaskType);
                    }));
        }

        private void fillTaskBallparkValueOfFeatureBallparkRows() {
            featureBallparks.values().stream().forEach(ballparkRow -> {
                Issue feature = issueBufferService.getIssueByKey(ballparkRow.taskNum);

                ballparkRow.taskBallpark = cluster.getClusterFor(ballparkRow.subtaskType, ballparkRow.tshirtSize)
                        .map(ci -> ci.getEffort())
                        .orElse(originalEstimateInHour(feature));
            });
        }

        private void resetTaskBallparkValueOfSubtaskRows(List<FromJiraDataRow> rows) {
            rows.stream().filter(r -> FromJiraDataRow.QUERY_TYPE_SUBTASK_PLAN.equals(r.queryType)).forEach(r -> r.taskBallpark = 0.0);
        }

        private void syncFeatureBallparks(List<FromJiraDataRow> rows) {
            List<FromJiraDataRow> removedBallparks = rows.stream()
                    .filter(r -> FromJiraDataRow.QUERY_TYPE_FEATURE_BALLPARK.equals(r.queryType))
                    .filter(r -> !featureBallparks.values().contains(r))
                    .collect(toList());
            
            rows.removeAll(removedBallparks);
            
            for (FromJiraDataRow ballparkRow : featureBallparks.values()) {
                if (!rows.contains(ballparkRow))
                    rows.add(ballparkRow);
            }
        }
        
        private void makeFeatureBallparks(List<FromJiraDataRow> subtaskRows) {
            for(FromJiraDataRow subtaskRow : subtaskRows) {
                Status featureStatus = issueStatusByName.get(subtaskRow.taskStatus);
                
                if (featureStatus == null) {
                    notFoundStatusNames.add(subtaskRow.taskStatus);

                } else if (jiraProperties.getFollowup().getFeatureStatusThatDontGenerateBallpark().contains(featureStatus.id)) {
                    continue;
                }

                Issue featureToday = issueBufferService.getIssueByKey(subtaskRow.taskNum);
                 
                if (featureToday == null) {
                    deletedIssues.add(subtaskRow.taskNum);
                    continue;
                }
                
                if (!featureToday.getIssueTypeName().equals(subtaskRow.taskType)) {
                    issuesWithChangedType.add(subtaskRow.taskNum);
                    continue;
                }
                
                List<BallparkMapping> mappingList = featureToday.getActiveBallparkMappings();

                for (BallparkMapping mapping : mappingList) {
                    String featureBallparkKey = featureBallparkKey(subtaskRow.taskNum, mapping);
                    
                    if (featureBallparks.containsKey(featureBallparkKey))
                        continue;
                    
                    FromJiraDataRow featureBallpark = createBallparkFeature(featureToday, subtaskRow, mapping, timezone);
                    featureBallparks.put(featureBallparkKey, featureBallpark);
                }
            }
        }

        private void overrideHistoryFile(FollowUpDataSnapshot originalSnapshot, List<FromJiraDataRow> newRows) {
            FromJiraDataSet newFromJiraDataSet = new FromJiraDataSet(originalSnapshot.getData().fromJiraDs.headers, newRows);

            FollowupData followupData = new FollowupData(
                    newFromJiraDataSet, 
                    originalSnapshot.getData().analyticsTransitionsDsList, 
                    originalSnapshot.getData().syntheticsTransitionsDsList);

            FollowUpDataSnapshot newSnapshot = new FollowUpDataSnapshot(originalSnapshot.getTimeline(), followupData, originalSnapshot.getCluster());
            
            try {
                historyRepository.save(projectKey, newSnapshot);

            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
        
        private void ensureMinimal(List<FromJiraDataRow> subtaskRows) {
            for(FromJiraDataRow subtaskRow : subtaskRows) {
                Long subtaskTypeId = issueTypesByName.get(subtaskRow.subtaskType).getId();
                Issue featureToday = issueBufferService.getIssueByKey(subtaskRow.taskNum);
                
                if (featureToday == null) {
                    deletedIssues.add(subtaskRow.taskNum);
                    continue;
                }
                
                if (!featureToday.getIssueTypeName().equals(subtaskRow.taskType)) {
                    issuesWithChangedType.add(subtaskRow.taskNum);
                    continue;
                }
                
                List<BallparkMapping> mappingList = featureToday.getActiveBallparkMappings();
                for (BallparkMapping mapping : mappingList) {
                    if (mapping.getJiraIssueTypes().contains(subtaskTypeId)) {
                        ensureMinimalFeatureEstimation(subtaskRow.taskNum, mapping, subtaskRow);
                    }
                }
            }
        }

        private void ensureMinimalFeatureEstimation(String featureIssueKey, BallparkMapping mapping, FromJiraDataRow subtaskRow) {
            String featureBallparkKey = featureBallparkKey(featureIssueKey, mapping);
            FromJiraDataRow featureBallpark = featureBallparks.get(featureBallparkKey);
            
            if (featureBallpark == null)
                return;
            
            FromJiraRowCalculation subtaskRowCalculation = fromJiraRowCalculator.calculate(subtaskRow);
            featureBallpark.taskBallpark -= subtaskRowCalculation.getEffortEstimate();
            
            if (featureBallpark.taskBallpark <= 0) {
                featureBallparks.remove(featureBallparkKey);
            }
        }

        private FromJiraDataRow createBallparkFeature(Issue feature, FromJiraDataRow subtaskRow, BallparkMapping ballparkMapping, ZoneId timezone) {
            FromJiraDataRow ballparkRow = new FromJiraDataRow();
            
            copyDemandFields(ballparkRow, subtaskRow);
            copyTaskFields(ballparkRow, subtaskRow);

            ballparkRow.planningType = "Ballpark";
            ballparkRow.project = subtaskRow.project;
            ballparkRow.tshirtSize = feature.getTshirtSizeOfSubtaskForBallpark(ballparkMapping);
            ballparkRow.worklog = 0.0;
            ballparkRow.wrongWorklog = subtaskRow.wrongWorklog;
            ballparkRow.queryType = FromJiraDataRow.QUERY_TYPE_FEATURE_BALLPARK;

            ballparkRow.demandDescription = subtaskRow.demandDescription;

            ballparkRow.taskRelease = subtaskRow.taskRelease;

            ballparkRow.subtaskType = ballparkMapping.getIssueType();
            ballparkRow.subtaskStatus = getBallparkStatus();
            ballparkRow.subtaskId = 0L;
            ballparkRow.subtaskNum = projectKey + "-0";
            ballparkRow.subtaskSummary = ballparkMapping.getIssueType();
            ballparkRow.subtaskDescription = issueDescription(0, subtaskRow.taskSummary);
            ballparkRow.subtaskFullDescription = issueFullDescription(ballparkMapping.getIssueType(), "", 0, subtaskRow.taskSummary);

            return ballparkRow;
        }

        private Double originalEstimateInHour(Issue issue) {
            if (issue == null)
                return 0.0;
            if (issue.getTimeTracking() == null)
                return 0.0;
            if (issue.getTimeTracking().getOriginalEstimateMinutes() == null)
                return 0.0;
            return issue.getTimeTracking().getOriginalEstimateMinutes()/60.0;
        }

        private void copyDemandFields(FromJiraDataRow targetRow, FromJiraDataRow sourceRow) {
            targetRow.demandId = sourceRow.demandId;
            targetRow.demandType = sourceRow.demandType;
            targetRow.demandStatus = sourceRow.demandStatus;
            targetRow.demandNum = sourceRow.demandNum;
            targetRow.demandSummary = sourceRow.demandSummary;
            targetRow.demandStatusPriority = sourceRow.demandStatusPriority;
            targetRow.demandPriorityOrder = sourceRow.demandPriorityOrder;
            targetRow.demandUpdatedDate = sourceRow.demandUpdatedDate;
            targetRow.demandBallpark = sourceRow.demandBallpark;
            targetRow.demandStartDateStepMillis = sourceRow.demandStartDateStepMillis;
            targetRow.demandAssignee = sourceRow.demandAssignee;
            targetRow.demandDueDate = sourceRow.demandDueDate;
            targetRow.demandCreated = sourceRow.demandCreated;
            targetRow.demandLabels = sourceRow.demandLabels;
            targetRow.demandComponents = sourceRow.demandComponents;
            targetRow.demandReporter = sourceRow.demandReporter;
            targetRow.demandCoAssignees = sourceRow.demandCoAssignees;
            targetRow.demandClassOfService = sourceRow.demandClassOfService;
            targetRow.demandCycletime = sourceRow.demandCycletime;
            targetRow.demandIsBlocked = sourceRow.demandIsBlocked;
            targetRow.demandLastBlockReason = sourceRow.demandLastBlockReason;
        }

        private void copyTaskFields(FromJiraDataRow targetRow, FromJiraDataRow sourceRow) {
            targetRow.taskId = sourceRow.taskId;
            targetRow.taskType = sourceRow.taskType;
            targetRow.taskStatus = sourceRow.taskStatus;
            targetRow.taskNum = sourceRow.taskNum;
            targetRow.taskSummary = sourceRow.taskSummary;
            targetRow.taskStatusPriority = sourceRow.taskStatusPriority;
            targetRow.taskPriorityOrder = sourceRow.taskPriorityOrder;
            targetRow.taskDescription = sourceRow.taskDescription;
            targetRow.taskFullDescription = sourceRow.taskFullDescription;
            targetRow.taskAdditionalEstimatedHours = sourceRow.taskAdditionalEstimatedHours;
            targetRow.taskStartDateStepMillis = sourceRow.taskStartDateStepMillis;
            targetRow.taskAssignee = sourceRow.taskAssignee;
            targetRow.taskDueDate = sourceRow.taskDueDate;
            targetRow.taskCreated = sourceRow.taskCreated;
            targetRow.taskLabels = sourceRow.taskLabels;
            targetRow.taskComponents = sourceRow.taskComponents;
            targetRow.taskReporter = sourceRow.taskReporter;
            targetRow.taskCoAssignees = sourceRow.taskCoAssignees;
            targetRow.taskClassOfService = sourceRow.taskClassOfService;
            targetRow.taskUpdatedDate = sourceRow.taskUpdatedDate;
            targetRow.taskCycletime = sourceRow.taskCycletime;
            targetRow.taskIsBlocked = sourceRow.taskIsBlocked;
            targetRow.taskLastBlockReason = sourceRow.taskLastBlockReason;
        }

        private String getBallparkStatus() {
            return statusById.get(jiraProperties.getFollowup().getBallparkDefaultStatus()).name;
        }

        private String issueDescription(Integer issueNum, String description) {
            return issueDescription(null, issueNum, description);
        }

        private String issueDescription(String size, Integer issueNum, String description) {
            String sizePart = StringUtils.isEmpty(size)?"":size+" | ";
            return String.format("%s%05d - %s", sizePart, issueNum, description);
        }

        private String issueFullDescription(String issueType, String size, Integer issueNum, String description) {
            return issueType + " | " +issueDescription(size, issueNum, description);
        }
    }

    private static String featureBallparkKey(String featureIssueKey, BallparkMapping mapping) {
        return featureBallparkKey(featureIssueKey, mapping.getIssueType());
    }
    
    private static String featureBallparkKey(String featureIssueKey, String ballparkIssueKey) {
        return featureIssueKey + "|" + ballparkIssueKey;
    }
    
    public static <T, K, U> Collector<T, ?, Map<K, U>> toLinkedMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        BinaryOperator<U> merge = (u, v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); };
        return Collectors.toMap(keyMapper, valueMapper, merge, LinkedHashMap::new);
    }
}
