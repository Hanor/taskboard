package objective.taskboard.followup;

public class EffortProgressDataRow {

    public String date;
    public Double effortDone;
    public Double effortOnBacklog;

    public EffortProgressDataRow(String date, Double effortDone, Double effortOnBacklog) {
        this.date = date;
        this.effortDone = effortDone;
        this.effortOnBacklog = effortOnBacklog;
    }

}
