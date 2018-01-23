package objective.taskboard.jira.client;

public class JiraIssueLinkTypeDto {
    public static enum Direction {
        OUTBOUND,
        INBOUND
    }

    Direction direction;

    private String name;
    private String description;

    public JiraIssueLinkTypeDto(String name, String description, Direction direction) {
        this.name = name;
        this.description = description;
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
