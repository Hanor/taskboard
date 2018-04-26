package objective.taskboard.followup.impl;

import java.time.ZoneId;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import objective.taskboard.followup.FollowUpDataSnapshotService;

@Component
public class FollowUpDataHistoryGeneratorJSONFiles {

    private final FollowUpDataSnapshotService dataSnapshotService;
    private boolean isExecutingDataHistoryGenerate = false;

    @Autowired
    public FollowUpDataHistoryGeneratorJSONFiles(FollowUpDataSnapshotService dataSnapshotSerice) {
        this.dataSnapshotService = dataSnapshotSerice;
    }

    @PostConstruct
    public void initialize() {
        dataSnapshotService.syncSynthesis(ZoneId.systemDefault());
        scheduledGenerate();
    }

    @Scheduled(cron = "${jira.followup.executionDataHistoryGenerator.cron}", zone = "${jira.followup.executionDataHistoryGenerator.timezone}")
    public synchronized void scheduledGenerate() {
        if (isExecutingDataHistoryGenerate)
            return;

        isExecutingDataHistoryGenerate = true;
        Thread thread = new Thread(() -> {
            try {
                dataSnapshotService.generateHistory(ZoneId.systemDefault());
            } finally {
                isExecutingDataHistoryGenerate = false;
            }
        });
        thread.setName(getClass().getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }
}
