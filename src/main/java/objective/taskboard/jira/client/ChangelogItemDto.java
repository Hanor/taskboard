package objective.taskboard.jira.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ChangelogItemDto {

    private String field;
    private String to;
    private String fromString;
    private String toString;

    public String getField() {
        return field;
    }

    public String getTo() {
        return to;
    }

    public String getFromString() {
        return fromString;
    }

    public String getToString() {
        return toString;
    }

}
