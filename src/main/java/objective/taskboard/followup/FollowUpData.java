package objective.taskboard.followup;

import java.util.List;

public class FollowUpData {

    public enum Version {

        VERSION_1("1"),
        VERSION_2("2");

        public final String value;

        Version(String value) {
            this.value = value;
        }
    }

    public final String followupDataVersion;
    public final FromJiraDataSet fromJiraDs;
    public final List<AnalyticsTransitionsDataSet> analyticsTransitionsDsList;
    public final List<SyntheticTransitionsDataSet> syntheticsTransitionsDsList;

    public FollowUpData(FromJiraDataSet fromJiraDs, List<AnalyticsTransitionsDataSet> analyticsTransitionsDsList, List<SyntheticTransitionsDataSet> syntheticsTransitionsDsList) {
        this(Version.VERSION_2, fromJiraDs, analyticsTransitionsDsList, syntheticsTransitionsDsList);
    }

    public FollowUpData(Version version, FromJiraDataSet fromJiraDs, List<AnalyticsTransitionsDataSet> analyticsTransitionsDsList, List<SyntheticTransitionsDataSet> syntheticsTransitionsDsList) {
        this.followupDataVersion = version.value;
        this.fromJiraDs = fromJiraDs;
        this.analyticsTransitionsDsList = analyticsTransitionsDsList;
        this.syntheticsTransitionsDsList = syntheticsTransitionsDsList;
    }
}