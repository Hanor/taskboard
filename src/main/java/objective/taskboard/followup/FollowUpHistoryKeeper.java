package objective.taskboard.followup;

import java.time.ZoneId;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FollowUpHistoryKeeper {

    private final FollowUpSnapshotService snapshotService;
    private boolean isExecutingDataHistoryGenerate = false;

    @Autowired
    public FollowUpHistoryKeeper(FollowUpSnapshotService snapshotSerice) {
        this.snapshotService = snapshotSerice;
    }

    @PostConstruct
    public void initialize() {
        snapshotService.syncSynthesis(ZoneId.systemDefault());
        generate();
    }

    @Scheduled(cron = "${jira.followup.executionDataHistoryGenerator.cron}", zone = "${jira.followup.executionDataHistoryGenerator.timezone}")
    public synchronized void generate() {
        if (isExecutingDataHistoryGenerate)
            return;

        isExecutingDataHistoryGenerate = true;
        Thread thread = new Thread(() -> {
            try {
                snapshotService.storeSnapshots(ZoneId.systemDefault());
            } finally {
                isExecutingDataHistoryGenerate = false;
            }
        });
        thread.setName(getClass().getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }
}
