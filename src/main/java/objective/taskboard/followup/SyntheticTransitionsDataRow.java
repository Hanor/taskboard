package objective.taskboard.followup;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import objective.taskboard.utils.DateTimeUtils;

public class SyntheticTransitionsDataRow implements TransitionDataRow {
    public final ZonedDateTime date;
    public final String issueType;
    public final List<Integer> amountOfIssueInStatus;
    
    public SyntheticTransitionsDataRow(ZonedDateTime date, String issueType, List<Integer> amountOfIssueInStatus) {
        this.date = date;
        this.issueType = issueType;
        this.amountOfIssueInStatus = amountOfIssueInStatus;
    }

    @Override
    public List<String> getAsStringList() {
        List<String> list = new LinkedList<>();
        list.add(DateTimeUtils.toStringExcelFormat(date));
        list.add(issueType);
        list.addAll(amountOfIssueInStatus.stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
        return list;
    }

}
