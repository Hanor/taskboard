package objective.taskboard.followup;

public class TshirtSizeDataRow {
    
    public String clusterName;
    public String tshirtSize;
    public String type;
    public Double effort;
    public Double cycle;

    public TshirtSizeDataRow(String clusterName, String tshirtSize, String type, Double effort, Double cycle) {
        this.clusterName = clusterName;
        this.tshirtSize = tshirtSize;
        this.type = type;
        this.effort = effort;
        this.cycle = cycle;
    }

}
