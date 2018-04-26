package objective.taskboard.testUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

import objective.taskboard.utils.Clock;

@Component
public class SystemClockMock implements Clock {

    private Instant now = ZonedDateTime.of(2017, 6, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant();

    @Override
    public Instant now() {
        return now;
    }
    
    public void setNow(Instant now) {
        this.now = now;
    }
    
    public void setNow(String nowIso) {
        setNow(Instant.parse(nowIso));
    }

}
