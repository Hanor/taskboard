package objective.taskboard.followup;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static objective.taskboard.Constants.FROMJIRA_HEADERS;
import static objective.taskboard.followup.FixedFollowUpSnapshotValuesProvider.emptyValuesProvider;
import static objective.taskboard.followup.FollowUpScopeByTypeDataProvider.BASELINE_BACKLOG;
import static objective.taskboard.followup.FollowUpScopeByTypeDataProvider.BASELINE_DONE;
import static objective.taskboard.followup.FollowUpScopeByTypeDataProvider.INTANGIBLE_BACKLOG;
import static objective.taskboard.followup.FollowUpScopeByTypeDataProvider.INTANGIBLE_DONE;
import static objective.taskboard.followup.FollowUpScopeByTypeDataProvider.NEW_SCOPE_BACKLOG;
import static objective.taskboard.followup.FollowUpScopeByTypeDataProvider.NEW_SCOPE_DONE;
import static objective.taskboard.followup.FollowUpScopeByTypeDataProvider.REWORK_BACKLOG;
import static objective.taskboard.followup.FollowUpScopeByTypeDataProvider.REWORK_DONE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.followup.cluster.FollowUpClusterItem;
import objective.taskboard.followup.cluster.FollowupCluster;
import objective.taskboard.followup.cluster.FollowupClusterImpl;

public class FollowUpScopeByTypeDataProviderTest {

    private static final Double EFFORT_ESTIMATE = 1D;
    private static final String PROJECT_KEY = "PROJECT_TEST";
    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    private final FromJiraRowService rowService = mock(FromJiraRowService.class);
    private final FollowUpSnapshotService snapshotService = mock(FollowUpSnapshotService.class);
    private final FollowUpScopeByTypeDataProvider subject = new FollowUpScopeByTypeDataProvider(snapshotService, rowService);
    private final FromJiraDataRow row = new FromJiraDataRow();

    private FollowUpScopeByTypeDataSet data;

    @Before
    public void setup() {
        row.taskBallpark = EFFORT_ESTIMATE;
        FollowupCluster fc = mock(FollowupCluster.class);
        when(fc.isEmpty()).thenReturn(false);
        
        FollowUpSnapshot snapshot = getSnapshot(LocalDate.of(2018, 1, 1), asList(row));
        when(snapshotService.get(Optional.empty(), ZONE_ID, PROJECT_KEY)).thenReturn(snapshot);
    }

    @Test
    public void ifIsBaselineBacklog_thenSumBaselineBacklog() {
        when(rowService.isBaselineBacklog(row)).thenReturn(true);

        data = subject.getScopeByTypeData(PROJECT_KEY, Optional.empty(), ZONE_ID);

        Double expectedOthers = 0D;
        assertEffortEstimateByType(INTANGIBLE_DONE, expectedOthers);
        assertEffortEstimateByType(INTANGIBLE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_DONE, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(REWORK_DONE, expectedOthers);
        assertEffortEstimateByType(REWORK_BACKLOG, expectedOthers);
        assertEffortEstimateByType(BASELINE_DONE, expectedOthers);
        assertEffortEstimateByType(BASELINE_BACKLOG, EFFORT_ESTIMATE);
    }

    @Test
    public void ifIsBaselineDone_thenSumBaselineDone() {
        when(rowService.isBaselineDone(row)).thenReturn(true);

        data = subject.getScopeByTypeData(PROJECT_KEY, Optional.empty(), ZONE_ID);

        Double expectedOthers = 0D;
        assertEffortEstimateByType(INTANGIBLE_DONE, expectedOthers);
        assertEffortEstimateByType(INTANGIBLE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_DONE, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(REWORK_DONE, expectedOthers);
        assertEffortEstimateByType(REWORK_BACKLOG, expectedOthers);
        assertEffortEstimateByType(BASELINE_DONE, EFFORT_ESTIMATE);
        assertEffortEstimateByType(BASELINE_BACKLOG, expectedOthers);
    }

    @Test
    public void ifIsReworkDone_thenSumReworkDone() {
        when(rowService.isRework(row)).thenReturn(true);
        when(rowService.isDone(row)).thenReturn(true);

        data = subject.getScopeByTypeData(PROJECT_KEY, Optional.empty(), ZONE_ID);

        Double expectedOthers = 0D;
        assertEffortEstimateByType(INTANGIBLE_DONE, expectedOthers);
        assertEffortEstimateByType(INTANGIBLE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_DONE, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(REWORK_DONE, EFFORT_ESTIMATE);
        assertEffortEstimateByType(REWORK_BACKLOG, expectedOthers);
        assertEffortEstimateByType(BASELINE_DONE, expectedOthers);
        assertEffortEstimateByType(BASELINE_BACKLOG, expectedOthers);
    }

    @Test
    public void ifIsReworkBacklog_thenSumReworkBacklog() {
        when(rowService.isRework(row)).thenReturn(true);
        when(rowService.isBacklog(row)).thenReturn(true);

        data = subject.getScopeByTypeData(PROJECT_KEY, Optional.empty(), ZONE_ID);

        Double expectedOthers = 0D;
        assertEffortEstimateByType(INTANGIBLE_DONE, expectedOthers);
        assertEffortEstimateByType(INTANGIBLE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_DONE, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(REWORK_DONE, expectedOthers);
        assertEffortEstimateByType(REWORK_BACKLOG, EFFORT_ESTIMATE);
        assertEffortEstimateByType(BASELINE_DONE, expectedOthers);
        assertEffortEstimateByType(BASELINE_BACKLOG, expectedOthers);
    }

    @Test
    public void ifIsNewScopeDone_thenSumNewScopeDone() {
        when(rowService.isNewScope(row)).thenReturn(true);
        when(rowService.isDone(row)).thenReturn(true);

        data = subject.getScopeByTypeData(PROJECT_KEY, Optional.empty(), ZONE_ID);

        Double expectedOthers = 0D;
        assertEffortEstimateByType(INTANGIBLE_DONE, expectedOthers);
        assertEffortEstimateByType(INTANGIBLE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_DONE, EFFORT_ESTIMATE);
        assertEffortEstimateByType(NEW_SCOPE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(REWORK_DONE, expectedOthers);
        assertEffortEstimateByType(REWORK_BACKLOG, expectedOthers);
        assertEffortEstimateByType(BASELINE_DONE, expectedOthers);
        assertEffortEstimateByType(BASELINE_BACKLOG, expectedOthers);
    }

    @Test
    public void ifIsNewScopeBacklog_thenSumNewScopeBacklog() {
        when(rowService.isNewScope(row)).thenReturn(true);
        when(rowService.isBacklog(row)).thenReturn(true);

        data = subject.getScopeByTypeData(PROJECT_KEY, Optional.empty(), ZONE_ID);

        Double expectedOthers = 0D;
        assertEffortEstimateByType(INTANGIBLE_DONE, expectedOthers);
        assertEffortEstimateByType(INTANGIBLE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_DONE, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_BACKLOG, EFFORT_ESTIMATE);
        assertEffortEstimateByType(REWORK_DONE, expectedOthers);
        assertEffortEstimateByType(REWORK_BACKLOG, expectedOthers);
        assertEffortEstimateByType(BASELINE_DONE, expectedOthers);
        assertEffortEstimateByType(BASELINE_BACKLOG, expectedOthers);
    }

    @Test
    public void ifIsIntangibleDone_thenSumIntangibleDone() {
        when(rowService.isIntangible(row)).thenReturn(true);
        when(rowService.isDone(row)).thenReturn(true);

        data = subject.getScopeByTypeData(PROJECT_KEY, Optional.empty(), ZONE_ID);

        Double expectedOthers = 0D;
        assertEffortEstimateByType(INTANGIBLE_DONE, EFFORT_ESTIMATE);
        assertEffortEstimateByType(INTANGIBLE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_DONE, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(REWORK_DONE, expectedOthers);
        assertEffortEstimateByType(REWORK_BACKLOG, expectedOthers);
        assertEffortEstimateByType(BASELINE_DONE, expectedOthers);
        assertEffortEstimateByType(BASELINE_BACKLOG, expectedOthers);
    }

    @Test
    public void ifIsIntangible_thenSumIntangible() {
        when(rowService.isIntangible(row)).thenReturn(true);
        when(rowService.isBacklog(row)).thenReturn(true);

        data = subject.getScopeByTypeData(PROJECT_KEY, Optional.empty(), ZONE_ID);

        Double expectedOthers = 0D;
        assertEffortEstimateByType(INTANGIBLE_DONE, expectedOthers);
        assertEffortEstimateByType(INTANGIBLE_BACKLOG, EFFORT_ESTIMATE);
        assertEffortEstimateByType(NEW_SCOPE_DONE, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(REWORK_DONE, expectedOthers);
        assertEffortEstimateByType(REWORK_BACKLOG, expectedOthers);
        assertEffortEstimateByType(BASELINE_DONE, expectedOthers);
        assertEffortEstimateByType(BASELINE_BACKLOG, expectedOthers);
    }

    @Test
    public void ifMultipleRows_thenSumAllRows() {
        List<FromJiraDataRow> rows = asList(row, row, row);
        FollowUpSnapshot snapshot = getSnapshot(LocalDate.of(2018, 1, 1), rows);
        when(rowService.isBaselineBacklog(row)).thenReturn(true);
        when(snapshotService.get(Optional.empty(), ZONE_ID, PROJECT_KEY)).thenReturn(snapshot);

        data = subject.getScopeByTypeData(PROJECT_KEY, Optional.empty(), ZONE_ID);

        Double expectedOthers = 0D;
        Double expectedTwoRows = EFFORT_ESTIMATE * rows.size();
        assertEffortEstimateByType(INTANGIBLE_DONE, expectedOthers);
        assertEffortEstimateByType(INTANGIBLE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_DONE, expectedOthers);
        assertEffortEstimateByType(NEW_SCOPE_BACKLOG, expectedOthers);
        assertEffortEstimateByType(REWORK_DONE, expectedOthers);
        assertEffortEstimateByType(REWORK_BACKLOG, expectedOthers);
        assertEffortEstimateByType(BASELINE_DONE, expectedOthers);
        assertEffortEstimateByType(BASELINE_BACKLOG, expectedTwoRows);
    }

    @Test
    public void ifHasValues_thenTotalIsTheSumOfAllEffortEstimateValues() {
        List<FromJiraDataRow> rows = asList(row, row, row);
        FollowUpSnapshot snapshot = getSnapshot(LocalDate.of(2018, 1, 1), rows);
        when(rowService.isBaselineBacklog(row)).thenReturn(true);
        when(snapshotService.get(Optional.empty(), ZONE_ID, PROJECT_KEY)).thenReturn(snapshot);

        data = subject.getScopeByTypeData(PROJECT_KEY, Optional.empty(), ZONE_ID);

        Double total = data.values.stream().mapToDouble(i -> i.effortEstimate).sum();
        assertEquals(total, data.total);
    }

    @Test
    public void ifHasValues_thenFillAllInformationFields() {
        data = subject.getScopeByTypeData(PROJECT_KEY, Optional.empty(), ZONE_ID);
        assertEquals(PROJECT_KEY, data.projectKey);
        assertEquals(ZONE_ID.getId(), data.zoneId);
    }

    private static FollowUpSnapshot getSnapshot(LocalDate date, List<FromJiraDataRow> rows) {
        FromJiraDataSet dataSet = new FromJiraDataSet(FROMJIRA_HEADERS, rows);
        FollowUpData followupData = new FollowUpData(dataSet, emptyList(), emptyList());
        FollowupCluster fc = new FollowupClusterImpl(
                asList(new FollowUpClusterItem(mock(ProjectFilterConfiguration.class), "a", "a", "a", 1.0, 1.0)));
        return new FollowUpSnapshot(new FollowUpTimeline(date), followupData, fc, emptyValuesProvider());
    }

    private void assertEffortEstimateByType(String type, Double exceptedValue) {
        FollowUpScopeByTypeDataItem item = data.values.stream().filter(v -> type.equals(v.type)).findFirst().orElse(null);
        assertEquals(exceptedValue, item.effortEstimate);
    }
}
