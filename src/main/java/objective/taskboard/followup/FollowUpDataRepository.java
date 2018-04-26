package objective.taskboard.followup;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public interface FollowUpDataRepository {

    FollowupData get(LocalDate date, ZoneId timezone, String projectKey);
    List<LocalDate> getHistoryByProject(String projectKey);
    void save(String projectKey, LocalDate date, FollowupData data);

}