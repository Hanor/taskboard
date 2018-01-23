package objective.taskboard.jira.client;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ChangelogGroupDto {
    
    private JiraUserDto author;
    @JsonDeserialize(using=JodaDateTimeDeserializer.class)
    private DateTime created;
    private ArrayList<ChangelogItemDto> items;

    public ChangelogGroupDto(){
        
    }

    public ChangelogGroupDto(JiraUserDto author, DateTime created, ArrayList<ChangelogItemDto> items) {
        this.author = author;
        this.created = created;
        this.items = items;
        
    }

    public List<ChangelogItemDto> getItems() {
        return items;
    }

    public JiraUserDto getAuthor() {
        return author;
    }

    public DateTime getCreated() {
        return created;
    }

}
