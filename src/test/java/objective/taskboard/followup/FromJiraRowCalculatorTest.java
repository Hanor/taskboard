package objective.taskboard.followup;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Test;

import objective.taskboard.followup.FromJiraRowCalculator.FromJiraRowCalculation;
import objective.taskboard.followup.cluster.FollowUpClusterItem;
import objective.taskboard.followup.data.Template;

public class FromJiraRowCalculatorTest {
    
    private Template followUpConfig = mock(Template.class);

    private List<FollowUpClusterItem> clusterItems = asList(
            new FollowUpClusterItem(followUpConfig, "Alpha Test",   "unused", "S", 1d, 1.5d),
            new FollowUpClusterItem(followUpConfig, "Alpha Test",   "unused", "M", 2d, 2.5d),
            new FollowUpClusterItem(followUpConfig, "Alpha Test",   "unused", "L", 3d, 3.5d),
            
            new FollowUpClusterItem(followUpConfig, "Alpha Test",   "unused",    "S", 2d, 2.5d),
            new FollowUpClusterItem(followUpConfig, "Alpha Test",   "unused",    "M", 3d, 3.5d),
            new FollowUpClusterItem(followUpConfig, "Alpha Test",   "unused",    "L", 4d, 4.5d),
            
            new FollowUpClusterItem(followUpConfig, "Backend Dev",  "unused", "S", 5d, 5.5d),
            new FollowUpClusterItem(followUpConfig, "Backend Dev",  "unused", "M", 6d, 6.5d),
            new FollowUpClusterItem(followUpConfig, "Backend Dev",  "unused", "L", 7d, 7.5d));
    
    private FromJiraRowCalculator subject = new FromJiraRowCalculator(clusterItems);

    @Test
    public void calculateEffortEstimate_fromBallpark() {
        FromJiraDataRow row = new FromJiraDataRow();
        row.taskBallpark = 10d;
        row.queryType = "FEATURE BALLPARK";

        assertEquals(10d, subject.calculate(row).getEffortEstimate(), 0d);
    }
    
    @Test
    public void calculateEffortEstimate_subtaskWithWronglyFilledTaskBallpark() {
        FromJiraDataRow row = new FromJiraDataRow();
        row.taskBallpark = 10d;
        row.queryType = "SUBTASK PLAN";
        row.subtaskType = "Alpha Test";
        row.tshirtSize = "M";

        assertEquals(2d, subject.calculate(row).getEffortEstimate(), 0d);
    }
    
    @Test
    public void calculateEffortEstimate_fromCluster() {
        FromJiraDataRow row = new FromJiraDataRow();
        row.queryType = "SUBTASK PLAN";
        row.subtaskType = "Alpha Test";
        row.tshirtSize = "M";

        assertEquals(2d, subject.calculate(row).getEffortEstimate(), 0d);
    }
    
    @Test
    public void calculateEffortEstimate_noMatchingCluster() {
        FromJiraDataRow row = new FromJiraDataRow();
        row.queryType = "SUBTASK PLAN";
        row.subtaskType = "UX";
        row.tshirtSize = "M";

        assertEquals(0d, subject.calculate(row).getEffortEstimate(), 0d);
    }
    
    @Test
    public void calculate_emptyRow() {
        FromJiraDataRow row = new FromJiraDataRow();

        FromJiraRowCalculation calculation = subject.calculate(row);
        assertEquals(0d, calculation.getEffortEstimate(), 0d);
        assertEquals(0d, calculation.getEffortDone(), 0d);
        assertEquals(0d, calculation.getEffortOnBacklog(), 0d);
    }

    @Test
    public void calculateEffortDone_doneSubtask() {
        FromJiraDataRow row = new FromJiraDataRow();
        row.queryType = "SUBTASK PLAN";
        row.subtaskType = "Alpha Test";
        row.subtaskStatus = "Done";
        row.tshirtSize = "M";

        assertEquals(2d, subject.calculate(row).getEffortDone(), 0d);
    }
    
    @Test
    public void calculateEffortDone_cancelledSubtask() {
        FromJiraDataRow row = new FromJiraDataRow();
        row.queryType = "SUBTASK PLAN";
        row.subtaskType = "Alpha Test";
        row.subtaskStatus = "Cancelled";
        row.tshirtSize = "M";

        assertEquals(2d, subject.calculate(row).getEffortDone(), 0d);
    }
    
    @Test
    public void calculateEffortDone_doingSubtask() {
        FromJiraDataRow row = new FromJiraDataRow();
        row.queryType = "SUBTASK PLAN";
        row.subtaskType = "Alpha Test";
        row.subtaskStatus = "Doing";
        row.tshirtSize = "M";

        assertEquals(0d, subject.calculate(row).getEffortDone(), 0d);
    }
    
    @Test
    public void calculateEffortOnBacklog_doneSubtask() {
        FromJiraDataRow row = new FromJiraDataRow();
        row.queryType = "SUBTASK PLAN";
        row.subtaskType = "Alpha Test";
        row.subtaskStatus = "Done";
        row.tshirtSize = "M";

        assertEquals(0d, subject.calculate(row).getEffortOnBacklog(), 0d);
    }
    
    @Test
    public void calculateEffortOnBacklog_doingSubtask() {
        FromJiraDataRow row = new FromJiraDataRow();
        row.queryType = "SUBTASK PLAN";
        row.subtaskType = "Alpha Test";
        row.subtaskStatus = "Doing";
        row.tshirtSize = "M";

        assertEquals(2d, subject.calculate(row).getEffortOnBacklog(), 0d);
    }
}
