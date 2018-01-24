package objective.taskboard.data;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.junit.Test;

public class IssueTest {
    @Test
    public void ensureAllScratchFieldsAreCopied() throws IllegalArgumentException, IllegalAccessException {
        IssueScratch issueScratch = new IssueScratch(
                66l,
                "K-66",
                "K",
                "A project",
                1l,
                "icon-uri",
                "summary", 
                2l,
                3000l,
                "co1",
                "co2",
                "K", 
                4l,
                "parent-type-uri",
                Arrays.asList("x","y"), 
                "subresponsaveis", 
                "assignee",
                13l,
                new Date(),
                6000l,
                new Date(),
                new Date(),
                "description",
                "comments", 
                Arrays.asList("labels"),
                Arrays.asList("components"), 
                new LinkedHashMap<String, Serializable>(),
                90l,
                new TaskboardTimeTracking(), 
                "reporter",
                new LinkedList<>(), 
                new CustomField("classOfService", null),
                "releaseId",
                new LinkedList<>(),
                new LinkedList<>()
                );

        Issue subject = new Issue(issueScratch, null, null, null, null, null, null, null, null, null);

        Field[] declaredFields = IssueScratch.class.getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            assertEquals(field.get(issueScratch), field.get(subject));
        }
    }
}