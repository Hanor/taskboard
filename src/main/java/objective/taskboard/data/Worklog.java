package objective.taskboard.data;

import java.util.Date;

import objective.taskboard.jira.client.JiraWorklogDto;

public class Worklog {

    public Date started;
    public int timeSpentSeconds;
    public String author;

    public static Worklog from(JiraWorklogDto w) {
        Worklog r = new Worklog();
        r.author = w.author.getName();
        r.started = w.started;
        r.timeSpentSeconds = w.timeSpentSeconds;

        return r;
    }
}
