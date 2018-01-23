package objective.taskboard.jira.data;

import java.util.List;
import java.util.Map;

public class Transition {

    public Long id;
    public String name;
    public Status to;
    public Map<String, Field> fields;
    
    public Transition(){}

    public Transition(Long id, String name, Status to, Map<String, Field> fields) {
        this.id = id;
        this.name = name;
        this.to = to;
        this.fields = fields;
    }

    public static class Field {
        public String name;
        public Boolean required;
        public List<Object> allowedValues;
        public String defaultValue;
        public Boolean hasDefaultValue;
    }

}