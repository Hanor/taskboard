package objective.taskboard.followup;

import java.time.LocalDate;

import objective.taskboard.domain.FollowupDailySynthesis;
import objective.taskboard.followup.FromJiraRowCalculator.FromJiraRowCalculation;

public class EffortHistoryRow {
    public final LocalDate date;
    public double sumEffortDone = 0;
    public double sumEffortBacklog = 0;

    public EffortHistoryRow(LocalDate date) {
        this.date = date;
    }
    
    public EffortHistoryRow(LocalDate date, double sumEffortDone, double sumEffortBacklog) {
        this.date = date;
        this.sumEffortDone = sumEffortDone;
        this.sumEffortBacklog = sumEffortBacklog;
    }
    
    public double progress() {
        double total = sumEffortDone + sumEffortBacklog;
        double d = sumEffortDone / total;
        if (Double.isNaN(d))
            return 0.0;
        return d;
    }

    public static EffortHistoryRow from(FollowupDailySynthesis dailySynthesis) {
        return new EffortHistoryRow(dailySynthesis.getFollowupDate(), dailySynthesis.getSumEffortDone(), dailySynthesis.getSumEffortBacklog());
    }
    
    public static EffortHistoryRow from(LocalDate date, FromJiraRowCalculation sumCalculation) {
        return new EffortHistoryRow(date, sumCalculation.getEffortDone(), sumCalculation.getEffortOnBacklog());
    }
}