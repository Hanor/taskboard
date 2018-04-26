package objective.taskboard.followup;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import objective.taskboard.utils.DateTimeUtils;

public class AnalyticsTransitionsDataRow implements TransitionDataRow {
    public final String issueKey;
    public final String issueType;
    public final List<ZonedDateTime> transitionsDates;
    
    public AnalyticsTransitionsDataRow(String issueKey, String issueType, List<ZonedDateTime> lastTransitionDate) {
        this.issueKey = issueKey;
        this.issueType = issueType;
        this.transitionsDates = lastTransitionDate;
    }

    @Override
    public List<String> getAsStringList() {
        List<String> list = new LinkedList<>();
        list.add(issueKey);
        list.add(issueType);
        list.addAll(transitionsDates.stream()
                .map(DateTimeUtils::toStringExcelFormat)
                .collect(Collectors.toList()));
        return list;
    }

}