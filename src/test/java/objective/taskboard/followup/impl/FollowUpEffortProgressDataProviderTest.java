package objective.taskboard.followup.impl;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import objective.taskboard.followup.EffortProgressDataRow;
import objective.taskboard.followup.EffortProgressDataSet;
import objective.taskboard.followup.FollowUpHelper;
import objective.taskboard.followup.FollowupData;

public class FollowUpEffortProgressDataProviderTest {

    @Test
    public void givenNoHistoricalFollowUp_ReturnOneDate() {
        String date = new Date().toString();
        FollowupData followupData = FollowUpHelper.getDefaultFollowupData();
        EffortProgressDataSet effortProgressDs = FollowUpEffortProgressDataProvider.getEffortProgressDs(date, followupData.fromJiraDs);

        assertHeaders(effortProgressDs.headers);
        assertEquals(1, effortProgressDs.rows.size());

        EffortProgressDataRow expectedRow = new EffortProgressDataRow(date, 0.00, 1.00);
        EffortProgressDataRow actualRow = effortProgressDs.rows.get(0);
        assertRow(expectedRow, actualRow);
    }

    @Test
    public void givenNoHistoricalFollowUp_AndEmptyCurrentFollowUpData_ReturnNoRows() {
        FollowupData followupData = FollowUpHelper.getEmptyFollowupData();
        EffortProgressDataSet effortProgressDs = FollowUpEffortProgressDataProvider.getEffortProgressDs(new Date().toString(), followupData.fromJiraDs);

        assertHeaders(effortProgressDs.headers);
        assertEquals(0, effortProgressDs.rows.size());
    }

    @Test
    public void givenMultiplesHistoricalFollowUp_ReturnAllDates() {
        // TODO
        FollowupData followupData = FollowUpHelper.getEmptyFollowupData();
        EffortProgressDataSet effortProgressDs = FollowUpEffortProgressDataProvider.getEffortProgressDs(new Date().toString(), followupData.fromJiraDs);

        assertHeaders(effortProgressDs.headers);
        assertEquals(4, effortProgressDs.rows.size());
    }

    public void assertHeaders(List<String> effortProgressDsHeaders) {
        List<String> headers = Arrays.asList("Date", "effortDoneTotal", "effortBacklogTotal");
        assertEquals(headers, effortProgressDsHeaders);
    }

    public void assertRow(EffortProgressDataRow expectedRow, EffortProgressDataRow actualRow) {
        assertEquals(expectedRow.date, actualRow.date);
        assertEquals(expectedRow.effortDone, actualRow.effortDone);
        assertEquals(expectedRow.effortOnBacklog, actualRow.effortOnBacklog);
    }

}
