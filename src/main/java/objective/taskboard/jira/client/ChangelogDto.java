package objective.taskboard.jira.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ChangelogDto {
    private List<ChangelogGroupDto> histories;

    public List<ChangelogGroupDto> getHistories() {
        return histories;
    }

}
