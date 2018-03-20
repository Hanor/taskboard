package objective.taskboard.data;

import java.io.Serializable;

public class Subtask implements Serializable {
    private static final long serialVersionUID = -1394125985489540555L;
    public String issueTypeUri;
    public String issueKey;
    public String summary;
    public String status;
    public String color;
    public String issueType;
    
    public Subtask(){}
    
    public Subtask(String key, String summary, String status, String issueType, String issueTypeUri, String color) {
        this.issueKey = key;
        this.summary = summary;
        this.status = status;
        this.issueType = issueType;
        this.issueTypeUri = issueTypeUri;
        this.color = color;
    }
}
