package objective.taskboard.spreadsheet;

import objective.taskboard.followup.FollowUpHelper;
import objective.taskboard.followup.FromJiraDataRow;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Map;

import static objective.taskboard.utils.IOUtilities.resourceToString;
import static org.junit.Assert.assertEquals;

public class SharedStringsEditorTest extends AbstractEditorTest {

    private static final String MSG_ASSERT_SHARED_STRINGS_SIZE = "Shared strings size";

    @Test
    public void getSharedStringsInitialTest() throws ParserConfigurationException, SAXException, IOException {
        try (SimpleSpreadsheetEditor editor = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            editor.open();
            SharedStringsEditor subject = editor.sharedStringsEditor;

            Map<String, Long> sharedStrings = subject.getSharedStrings();
            assertEquals(MSG_ASSERT_SHARED_STRINGS_SIZE, 204, sharedStrings.size());
            assertEquals("First shared string", 0, sharedStrings.get("project").longValue());
            assertEquals("Some special character shared string", 44, sharedStrings.get("Demand Status > Demand > Task Status > Task > Subtask").longValue());
            assertEquals("Any shared string", 126, sharedStrings.get("Group %").longValue());
            assertEquals("Last shared string", 203, sharedStrings.get("BALLPARK").longValue());
        }
    }

    @Test
    public void addNewSharedStringsInTheEndTest() throws ParserConfigurationException, SAXException, IOException {
        try (SimpleSpreadsheetEditor editor = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            editor.open();
            SharedStringsEditor subject = editor.sharedStringsEditor;

            Map<String, Long> sharedStrings = subject.getSharedStrings();
            assertEquals(MSG_ASSERT_SHARED_STRINGS_SIZE, 204, sharedStrings.size());
            assertEquals("First shared string", 0, sharedStrings.get("project").longValue());
            assertEquals("Last shared string", 203, sharedStrings.get("BALLPARK").longValue());

            SimpleSpreadsheetEditor.Sheet sheet = editor.getSheet("From Jira");
            for (FromJiraDataRow followUpData : FollowUpHelper.getDefaultFromJiraDataRowList())
                addRow(sheet, followUpData);
            sheet.save();

            assertEquals(MSG_ASSERT_SHARED_STRINGS_SIZE, 218, sharedStrings.size());
            assertEquals("First new shared string", 204, sharedStrings.get("PROJECT TEST").longValue());
            assertEquals("Any new shared string", 210, sharedStrings.get("Summary Feature").longValue());
            assertEquals("Last new shared string", 217, sharedStrings.get("Full Description Sub-task").longValue());
        }
    }

    @Test
    public void generateSharedStringsInOrderTest() throws ParserConfigurationException, SAXException, IOException {
        try (SimpleSpreadsheetEditor editor = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            editor.open();
            SharedStringsEditor subject = editor.sharedStringsEditor;

            String sharedStringsGenerated = subject.generateSharedStrings();

            String sharedStringsExpected = resourceToString("followup/generateSharedStringsInOrderTest.xml");
            assertEquals("Shared strings", sharedStringsExpected, sharedStringsGenerated);
        }
    }

    @Test
    public void generateSharedStringsInOrderAfterAddNewSharedStringTest() throws ParserConfigurationException, SAXException, IOException {
        try (SimpleSpreadsheetEditor editor = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            editor.open();
            SharedStringsEditor subject = editor.sharedStringsEditor;

            Map<String, Long> sharedStrings = subject.getSharedStrings();

            assertEquals(MSG_ASSERT_SHARED_STRINGS_SIZE, 204, sharedStrings.size());

            SimpleSpreadsheetEditor.Sheet sheet = editor.getSheet("From Jira");
            for (FromJiraDataRow followUpData : FollowUpHelper.getDefaultFromJiraDataRowList())
                addRow(sheet, followUpData);

            sheet.save();

            assertEquals(MSG_ASSERT_SHARED_STRINGS_SIZE, 218, sharedStrings.size());

            String sharedStringsGenerated = subject.generateSharedStrings();

            String sharedStringsExpected = resourceToString("followup/generateSharedStringsInOrderAfterAddNewSharedStringTest.xml");
            assertEquals("Shared strings", sharedStringsExpected, sharedStringsGenerated);
        }
    }
}
