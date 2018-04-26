package objective.taskboard.followup;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import objective.taskboard.followup.FollowUpDataSnapshot.SnapshotRow;
import objective.taskboard.followup.FromJiraRowCalculator.FromJiraRowCalculation;

public class PlannedVsBallparkDataAccumulatorTest {
    
    private FollowUpDataSnapshot snapshot = mock(FollowUpDataSnapshot.class);
    private FollowUpDataSnapshotService snapshotService = mock(FollowUpDataSnapshotService.class);
    private PlannedVsBallparkDataAccumulator subject = new PlannedVsBallparkDataAccumulator(snapshotService);
    
    @Before
    public void setup() {
        when(snapshot.getSnapshotRows()).thenReturn(emptyList());
        when(snapshot.hasClusterConfiguration()).thenReturn(true);
        
        when(snapshotService.getFromCurrentState(any(), any())).thenReturn(snapshot);
    }

    @Test
    public void givenAnAccumulator_whenInvokeGetData_thenShouldReturnTwoItens() {
        List<PlannedVsBallparkChartData> result = subject.calculate("PX");
        assertThat(result, hasSize(2));
    }
    
    @Test
    public void givenNewAccumulator_whenHasNotAccumulate_thenPlannedTotalEffortShouldBeZero() {        
        PlannedVsBallparkChartData plannedData = getDataFromAccumulatorWithType(subject, "Planned");
        
        assertEquals(0d, plannedData.totalEffort, 0d);
    }
    
    @Test
    public void givenNewAccumulator_whenHasNotAccumulate_thenBallparkTotalEffortShouldBeZero() {        
        PlannedVsBallparkChartData plannedData = getDataFromAccumulatorWithType(subject, "Ballpark");
        
        assertEquals(0d, plannedData.totalEffort, 0d);
    }
    
    @Test
    public void givenSomeSnapshotRows_whenAccumulate_thenShouldSumEffortEstimateOfPlannedRows() {
        when(snapshot.getSnapshotRows()).thenReturn(getSnapshotRows());

        PlannedVsBallparkChartData plannedData = getDataFromAccumulatorWithType(subject, "Planned");        
        assertEquals(25d, plannedData.totalEffort, 0d);                
    }
    
    @Test
    public void givenSomeSnapshotRows_whenAccumulate_thenShouldSumEffortEstimateOfBallparkRows() {
        when(snapshot.getSnapshotRows()).thenReturn(getSnapshotRows());

        PlannedVsBallparkChartData ballparkData = getDataFromAccumulatorWithType(subject, "Ballpark");        
        assertEquals(45d, ballparkData.totalEffort, 0d);                
    }

    private List<SnapshotRow> getSnapshotRows() {
        FromJiraDataRow plannedRow = new FromJiraDataRow();
        plannedRow.queryType = FromJiraDataRow.QUERY_TYPE_SUBTASK_PLAN;
        
        FromJiraDataRow ballparkRow1 = new FromJiraDataRow();
        ballparkRow1.queryType = FromJiraDataRow.QUERY_TYPE_DEMAND_BALLPARK;
        
        FromJiraDataRow ballparkRow2 = new FromJiraDataRow();
        ballparkRow2.queryType = FromJiraDataRow.QUERY_TYPE_FEATURE_BALLPARK;
        
        List<SnapshotRow> snapshotRows = Arrays.asList(
                new SnapshotRow(plannedRow, new FromJiraRowCalculation(10d, 0d, 0d)),
                new SnapshotRow(plannedRow, new FromJiraRowCalculation(15d, 0d, 0d)),
                new SnapshotRow(ballparkRow1, new FromJiraRowCalculation(20d, 0d, 0d)),
                new SnapshotRow(ballparkRow1, new FromJiraRowCalculation(25d, 0d, 0d)));
        return snapshotRows;
    }
    
    private PlannedVsBallparkChartData getDataFromAccumulatorWithType(PlannedVsBallparkDataAccumulator accumulator, String type) {
        return accumulator.calculate("PX").stream().filter(data -> data.type.equals(type)).findFirst().get();
    }
}