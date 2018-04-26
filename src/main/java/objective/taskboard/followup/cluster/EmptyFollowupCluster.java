package objective.taskboard.followup.cluster;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class EmptyFollowupCluster implements FollowupCluster {

    @Override
    public Optional<FollowUpClusterItem> getClusterFor(String subtaskType, String tshirtSize) {
        return Optional.empty();
    }

    @Override
    public List<FollowUpClusterItem> getClusterItems() {
        return Collections.emptyList();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}
