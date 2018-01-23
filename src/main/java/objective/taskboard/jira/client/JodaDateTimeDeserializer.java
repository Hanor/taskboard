package objective.taskboard.jira.client;

import org.joda.time.DateTime;

import com.fasterxml.jackson.datatype.joda.cfg.FormatConfig;
import com.fasterxml.jackson.datatype.joda.cfg.JacksonJodaDateFormat;
import com.fasterxml.jackson.datatype.joda.deser.DateTimeDeserializer;


public class JodaDateTimeDeserializer  extends DateTimeDeserializer {
    private static final long serialVersionUID = 463579645291150129L;

    public JodaDateTimeDeserializer(Class<?> cls, JacksonJodaDateFormat format) {
        super(cls,format);
    }
    
    public JodaDateTimeDeserializer() {
        super(DateTime.class, FormatConfig.DEFAULT_DATETIME_PARSER);
    }
}
