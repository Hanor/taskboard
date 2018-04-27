package objective.taskboard.project;

import java.time.LocalDate;
import java.util.List;

public interface ProjectBaselineProvider {

    List<LocalDate> getAvailableDates(String projectKey);

}
