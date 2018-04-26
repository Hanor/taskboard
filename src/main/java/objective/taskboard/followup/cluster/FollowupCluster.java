package objective.taskboard.followup.cluster;

import java.util.List;
import java.util.Optional;

public interface FollowupCluster {
    Optional<FollowUpClusterItem> getClusterFor(String subtaskType, String tshirtSize);

    List<FollowUpClusterItem> getClusterItems();

    boolean isEmpty();
}
