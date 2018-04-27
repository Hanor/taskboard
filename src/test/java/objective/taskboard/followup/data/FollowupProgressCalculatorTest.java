package objective.taskboard.followup.data;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import objective.taskboard.followup.EffortHistoryRow;
import objective.taskboard.followup.FollowUpSnapshot;
import objective.taskboard.followup.FollowUpSnapshotService;
import objective.taskboard.followup.FollowUpTimeline;

public class FollowupProgressCalculatorTest {

    private static final String PROJECT = "PX";
    private static final ZoneId TIMEZONE = ZoneId.of("UTC");

    private FollowUpSnapshotService snapshotService = mock(FollowUpSnapshotService.class);
    private FollowupProgressCalculator subject = new FollowupProgressCalculator(snapshotService);

    @Test
    public void calculateWithEmptyHistory_shouldReturnEmptyActualAndProjectionShouldBeEqualToExpected() {
        currentSnapshot(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 10));
        
        ProgressData progressData = subject.calculate(TIMEZONE, PROJECT, 40);
        
        assertEquals(0, progressData.actual.size());
        assertEquals(0, progressData.actualProjection.size());
        assertEquals(0, progressData.expected.size());
    }

    @Test
    public void calculate_shouldCalculateEffort() {
        currentSnapshot(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 10),
                new EffortHistoryRow(LocalDate.of(2018, 1, 1),      1,       10)
                );

        ProgressData progressData = subject.calculate(TIMEZONE, PROJECT, 40);
        
        assertEquals(.091, progressData.actual.get(0).progress, 0.001);
        assertEquals(.091, progressData.actualProjection.get(0).progress, 0.001);
        assertEquals(.182, progressData.actualProjection.get(1).progress, 0.001);
    }
    
    @Test
    public void calculate_shouldCalculateExpectedEffortForAllDatesInTheRange() {
        currentSnapshot(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 10),
                new EffortHistoryRow(LocalDate.of(2018, 1, 1),      1,       10)
                );
        
        ProgressData progressData = subject.calculate(TIMEZONE, PROJECT, 40);
        
        List<ProgressDataPoint> expected = progressData.expected;
        assertEquals(10, expected.size());
        assertEquals(.0, expected.get(0).progress, 0.001);
        assertEquals(.11, expected.get(1).progress, 0.01);
        assertEquals(1.0, expected.get(expected.size()-1).progress, 0.001);
    }
    
    @Test
    public void calculate_whenDeliveryDateIsInThePast_ExpectedProgressPastDeliveryIsAlways100percent() {
        currentSnapshot(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 10), 
                new EffortHistoryRow(LocalDate.of(2018, 1, 1),  1,       10),
                new EffortHistoryRow(LocalDate.of(2018, 1, 11), 1,       10)
                );
        
        ProgressData progressData = subject.calculate(TIMEZONE, PROJECT, 40);
        
        List<ProgressDataPoint> expected = progressData.expected;
        assertEquals(11, expected.size());
        assertEquals(1.0, expected.get(expected.size()-2).progress, 0.001);
        assertEquals(1.0, expected.get(expected.size()-1).progress, 0.001);
    }
    
    @Test
    public void calculateWithOneSample_shouldCalculateProjectionBasedOnAvgOfLastEffort() {
        currentSnapshot(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 10),
                new EffortHistoryRow(LocalDate.of(2018, 1, 1),      0,       10),
                new EffortHistoryRow(LocalDate.of(2018, 1, 2),      0.7,     10)
                );
        
        ProgressData progressData = subject.calculate(TIMEZONE, PROJECT, 40);

        assertEquals(.065, progressData.actual.get(1).progress, 0.001);
        assertEquals(9, progressData.actualProjection.size());
        assertEquals(LocalDate.of(2018, 1, 2), progressData.actualProjection.get(0).date);
        assertEquals(LocalDate.of(2018, 1, 3), progressData.actualProjection.get(1).date);
        assertEquals(LocalDate.of(2018, 1, 6), progressData.actualProjection.get(4).date);
        assertEquals(progressData.actual.get(1).progress,  progressData.actualProjection.get(0).progress, 0.001);
        
        assertEquals(.065,  progressData.actualProjection.get(0).progress, 0.001);
        assertEquals(.13,  progressData.actualProjection.get(1).progress, 0.001);
        assertEquals(.327, progressData.actualProjection.get(4).progress, 0.001);
    }
    
    @Test
    public void calculateWhenDeliveryDateIsBeforeLastActualDate_projectionShouldBeEmptyAndEndDateMustMatchLastActualDate() {
        currentSnapshot(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 2),
                new EffortHistoryRow(LocalDate.of(2018, 1, 1), 0,   10),
                new EffortHistoryRow(LocalDate.of(2018, 1, 2), 0.7, 10),
                new EffortHistoryRow(LocalDate.of(2018, 1, 3), 1,   10)
                );
        
        ProgressData progressData = subject.calculate(TIMEZONE, PROJECT, 40);
        
        assertEquals(1, progressData.actualProjection.size());
        assertEquals(LocalDate.of(2018, 1, 3), progressData.endingDate);
    }
    
    
    @Test
    public void calculate_givenProjectionSize_shouldCalculateProjectionWithGivenSize() {
        currentSnapshot(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 10),
                new EffortHistoryRow(LocalDate.of(2018, 1, 1),      0,       10),
                new EffortHistoryRow(LocalDate.of(2018, 1, 2),      0.7,     9.3),
                new EffortHistoryRow(LocalDate.of(2018, 1, 3),      1.8,     8.2)
                );
        
        ProgressData progressData = subject.calculate(TIMEZONE, PROJECT, 2);
        
        assertEquals(.07, progressData.actual.get(1).progress, 0.001);
        assertEquals(8, progressData.actualProjection.size());
        assertEquals(LocalDate.of(2018, 1, 4), progressData.actualProjection.get(1).date);
        assertEquals(LocalDate.of(2018, 1, 6), progressData.actualProjection.get(3).date);
        assertEquals(progressData.actual.get(2).progress,  progressData.actualProjection.get(0).progress, 0.001);

        assertEquals(.29,  progressData.actualProjection.get(1).progress, 0.001);
    }

    @Test
    public void whenProjectionIsSteep_shouldCapAt100Percent() {
        currentSnapshot(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 7),
                new EffortHistoryRow(LocalDate.of(2018, 1, 1), 0, 10)
                , new EffortHistoryRow(LocalDate.of(2018, 1, 2), 2.5, 7.5)
        );

        ProgressData progressData = subject.calculate(TIMEZONE, PROJECT, 2);

        assertThat(progressData.actualProjection).allMatch(p -> p.progress <= 1.0);
        assertThat(progressData.actualProjection).extracting(p -> p.sumEffortDone)
                .containsExactly(2.5, 5.0, 7.5, 10.0, 10.0, 10.0);
        assertThat(progressData.actualProjection).extracting(p -> p.sumEffortBacklog)
                .containsExactly(7.5, 5.0, 2.5, 0.0, 0.0, 0.0);
    }

    @Test
    public void givenHistoryNearDaylightSavingTime_whenMissingHistoryDays_shouldInterpolateCorrectly() throws ParseException {
        currentSnapshot(LocalDate.of(2017, 10, 13), LocalDate.of(2017, 10, 20),
                new EffortHistoryRow(LocalDate.of(2017, 10, 13), 0, 10)
                , new EffortHistoryRow(LocalDate.of(2017, 10, 16), 3, 7)
        );

        ProgressData progressData = subject.calculate(TIMEZONE, PROJECT, 40);

        assertThat(progressData.actual).extracting(p -> p.date)
                .containsExactly(
                        LocalDate.of(2017, 10, 13)
                        , LocalDate.of(2017, 10, 14)
                        , LocalDate.of(2017, 10, 15)
                        , LocalDate.of(2017, 10, 16)
                );
    }

    @Test
    public void whenCalculate_backlogProjectionShouldBeMeanOfSamplesDelta() {
        currentSnapshot(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 7),
                new EffortHistoryRow(LocalDate.of(2018, 1, 1), 0, 7)
                , new EffortHistoryRow(LocalDate.of(2018, 1, 2), 0, 8)
                , new EffortHistoryRow(LocalDate.of(2018, 1, 3), 0, 10)
        );

        ProgressData progressData = subject.calculate(TIMEZONE, PROJECT, 3);

        assertThat(progressData.actualProjection).extracting(p -> p.sumEffortBacklog)
                .containsExactly(10.0, 11.5, 13.0, 14.5, 16.0);
    }
    
    private void currentSnapshot(LocalDate startDate, LocalDate endDate, EffortHistoryRow... history) {
        FollowUpTimeline timeline = new FollowUpTimeline(
                LocalDate.of(2019, 1, 1), 
                BigDecimal.ZERO, 
                Optional.of(startDate), 
                Optional.of(endDate), 
                Optional.empty());

        FollowUpSnapshot snapshot = mock(FollowUpSnapshot.class);
        when(snapshot.getTimeline()).thenReturn(timeline);
        when(snapshot.getEffortHistory()).thenReturn(asList(history));
        when(snapshot.hasClusterConfiguration()).thenReturn(true);
        
        when(snapshotService.getFromCurrentState(TIMEZONE, PROJECT)).thenReturn(snapshot);
    }
}