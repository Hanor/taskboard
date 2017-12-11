package objective.taskboard.followup.cluster;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.Validate;

import objective.taskboard.domain.TaskboardEntity;
import objective.taskboard.followup.data.Template;

@Entity
@Table(name = "followup_cluster_item")
public class FollowUpClusterItem extends TaskboardEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "followup_config", nullable = false)
    private Template followUpConfiguration;

    private String sizingFieldName;
    private String parentTypeName;
    private String sizing;
    private float effort;
    private float cycle;

    public FollowUpClusterItem(
            Template followUpConfiguration, 
            String sizingFieldName, 
            String parentTypeName,
            String sizing, 
            float effort, 
            float cycle) {
        
        setFollowUpConfiguration(followUpConfiguration);
        setSizingFieldName(sizingFieldName);
        setParentTypeName(parentTypeName);
        setSizing(sizing);
        setEffort(effort);
        setCycle(cycle);
    }

    public Template getFollowUpConfiguration() {
        return followUpConfiguration;
    }

    public void setFollowUpConfiguration(Template followUpConfiguration) {
        Validate.notNull(followUpConfiguration);
        this.followUpConfiguration = followUpConfiguration;
    }

    public String getSizingFieldName() {
        return sizingFieldName;
    }

    public void setSizingFieldName(String sizingFieldName) {
        Validate.notBlank(sizingFieldName);
        this.sizingFieldName = sizingFieldName;
    }

    public String getParentTypeName() {
        return parentTypeName;
    }

    public void setParentTypeName(String parentTypeName) {
        Validate.notBlank(parentTypeName);
        this.parentTypeName = parentTypeName;
    }

    public String getSizing() {
        return sizing;
    }

    public void setSizing(String sizing) {
        Validate.notBlank(sizing);
        this.sizing = sizing;
    }

    public Float getEffort() {
        return effort;
    }

    public void setEffort(float effort) {
        this.effort = effort;
    }

    public float getCycle() {
        return cycle;
    }

    public void setCycle(float cycle) {
        this.cycle = cycle;
    }
}
