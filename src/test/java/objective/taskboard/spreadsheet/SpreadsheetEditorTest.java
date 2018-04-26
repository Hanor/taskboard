package objective.taskboard.spreadsheet;

import static objective.taskboard.utils.IOUtilities.resourceToString;
import static objective.taskboard.utils.XmlUtils.normalizeXml;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.Streams;

import objective.taskboard.followup.FollowUpHelper;
import objective.taskboard.followup.FollowUpTemplate;
import objective.taskboard.followup.FromJiraDataRow;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor.SimpleSheet;
import objective.taskboard.utils.IOUtilities;
import objective.taskboard.utils.XmlUtils;

public class SpreadsheetEditorTest {
    private static final String PATH_FOLLOWUP_TEMPLATE = "followup/Followup-template.xlsm";

    private static final String MSG_ASSERT_SHARED_STRINGS_SIZE = "Shared strings size";

    @Test
    public void getSharedStringsInitialTest() throws ParserConfigurationException, SAXException, IOException {
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            subject.open();

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
        FollowUpTemplate template = getBasicTemplate();
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(template)) {
            subject.open();

            Map<String, Long> sharedStrings = subject.getSharedStrings();
            assertEquals(MSG_ASSERT_SHARED_STRINGS_SIZE, 204, sharedStrings.size());
            assertEquals("First shared string", 0, sharedStrings.get("project").longValue());
            assertEquals("Last shared string", 203, sharedStrings.get("BALLPARK").longValue());

            Sheet sheet = subject.getSheet("From Jira");
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
    public void addOneRowToSpreadSheet() throws IOException {
        FollowUpTemplate template = getBasicTemplate();
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(template)) {
            subject.open();

            SimpleSheet sheet = subject.getSheet("From Jira");
            sheet.truncate();
            
            SheetRow header = sheet.createRow();
            header.addColumn("project");
            header.addColumn("Backend Development");
            header.addColumn("Done");
            header.addColumn("To Review");
            header.addColumn("Cycle Hours per Role With Risk");
            header.addColumn("demand_type");

            addRow(sheet, FollowUpHelper.getDefaultFromJiraDataRow());
            sheet.save();

            String jiraDataSheet = subject.getSheet("From Jira").stringValue();
            String jiraDataSheetExpected = normalizeXml(resourceToString("followup/fromJiraWithOneRow.xml"));
            assertEquals("Jira data sheet", jiraDataSheetExpected, jiraDataSheet);
        }
    }
    
    @Test
    public void editRows() throws IOException {
        FollowUpTemplate template = getBasicTemplate();
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(template)) {
            subject.open();

            SimpleSheet sheet = subject.getSheet("Activities");

            SheetRow row = sheet.getOrCreateRow(1);
            row.setValue("A", "New Header");
            row.setValue("C", true);
            row.setValue("D", (String) null);
            
            row = sheet.getOrCreateRow(5);
            row.setValue("E", 50);
            row.setValue("J", 60);
            row.setValue("F", 51);
            row.addColumn(false);
            
            row = sheet.getOrCreateRow(3);
            row.setFormula("B", "NOW()");
            row.setValue("C", LocalDateTime.parse("2018-02-15T16:01"));
            row.setValue("D", LocalDate.parse("2018-02-15"));
            
            sheet.save();

            assertEquals(
                    normalizeXml(resourceToString("followup/activitiesEdited.xml")), 
                    subject.getSheet("Activities").stringValue());
        }
    }

    @Test
    public void generateSharedStringsInOrderTest() throws ParserConfigurationException, SAXException, IOException {
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            subject.open();

            String sharedStringsGenerated = subject.generateSharedStrings();

            String sharedStringsExpected = resourceToString("followup/generateSharedStringsInOrderTest.xml");
            assertEquals("Shared strings", sharedStringsExpected, sharedStringsGenerated);
        }
    }

    @Test
    public void generateSharedStringsInOrderAfterAddNewSharedStringTest() throws ParserConfigurationException, SAXException, IOException {
        FollowUpTemplate template = getBasicTemplate();
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(template)) {
            subject.open();

            Map<String, Long> sharedStrings = subject.getSharedStrings();

            assertEquals(MSG_ASSERT_SHARED_STRINGS_SIZE, 204, sharedStrings.size());

            Sheet sheet = subject.getSheet("From Jira");
            for (FromJiraDataRow followUpData : FollowUpHelper.getDefaultFromJiraDataRowList()) 
                addRow(sheet, followUpData);

            sheet.save();

            assertEquals(MSG_ASSERT_SHARED_STRINGS_SIZE, 218, sharedStrings.size());

            String sharedStringsGenerated = subject.generateSharedStrings();

            String sharedStringsExpected = resourceToString("followup/generateSharedStringsInOrderAfterAddNewSharedStringTest.xml");
            assertEquals("Shared strings", sharedStringsExpected, sharedStringsGenerated);
        }
    }

    @Test
    public void truncateFromJira() throws ParserConfigurationException, SAXException, IOException {
        FollowUpTemplate template = getBasicTemplate();
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(template)) {
            subject.open();

            SimpleSheet sheet = subject.getSheet("From Jira");
            sheet.truncate();
            String jiraDataSheet = sheet.stringValue();

            String jiraDataSheetExpected = normalizeXml(resourceToString("followup/emptyFromJira.xml"));

            assertEquals("Jira data sheet", jiraDataSheetExpected, jiraDataSheet);
        }
    }

    @Test
    public void getSheetByName_shouldFindTheSheetPart() throws IOException {
        try(SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            subject.open();
            Sheet sheet = subject.getSheet("T-shirt Size");
            String path = sheet.getSheetPath();

            assertEquals("xl/worksheets/sheet6.xml", path);
        }
    }

    @Test
    public void whenNewSheetIsAdded_updateExpectedFiles() throws Exception {
        FollowUpTemplate template = new FollowUpTemplate(resolve(PATH_FOLLOWUP_TEMPLATE));
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(template)) {
            subject.open();

            Sheet sheet = subject.createSheet("A New Sheet");
            assertEquals("xl/worksheets/sheet10.xml", sheet.getSheetPath());

            Sheet sheetAgain = subject.getSheet("A New Sheet");

            assertNotNull(sheetAgain);

            checkXmlElementInSpreadsheetFile(subject, 
                    "xl/_rels/workbook.xml.rels", 
                    "/Relationships/Relationship[@Id=\"rId20\"]", 
                    expectedAttributeValue("Target", "worksheets/sheet10.xml"));

            checkXmlElementInSpreadsheetFile(subject, 
                    "xl/workbook.xml", 
                    "/workbook/sheets/sheet[@sheetId=\"22\"]", 
                    expectedAttributeValue("name", "A New Sheet"),
                    expectedAttributeValue("r:id", "rId20"));

            checkXmlElementInSpreadsheetFile(subject, 
                    "[Content_Types].xml", 
                    "/Types/Override[@PartName=\"/xl/worksheets/sheet10.xml\"]", 
                    expectedAttributeValue("ContentType", "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"));
        }
    }

    @Test
    public void generateLotsOfLines() throws FileNotFoundException, IOException {
        FollowUpTemplate template = new FollowUpTemplate(resolve(PATH_FOLLOWUP_TEMPLATE));
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(template)) {
            subject.open();

            Sheet sheet = subject.createSheet("A New Sheet");

            for (int i = 0; i < 5000; i++) {
                addRow(sheet, FollowUpHelper.getDefaultFromJiraDataRow());
            }
            sheet.save();
        }
    }

    @Test
    public void openSpreadsheetShouldHaveFullRecalcOnLoadSet() throws IOException {
        FollowUpTemplate template = new FollowUpTemplate(resolve(PATH_FOLLOWUP_TEMPLATE));
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(template)) {
            subject.open();
            subject.save();

            NodeList calcPrList = XmlUtils.xpath(new File(subject.getExtractedSheetDirectory(), "xl/workbook.xml"), "/workbook/calcPr");
            Node calcPr = calcPrList.item(0);
            Node fullCalcOnLoad = calcPr.getAttributes().getNamedItem("fullCalcOnLoad");

            assertEquals("1", fullCalcOnLoad.getNodeValue());
        }
    }

    @Test
    public void whenCustomFormatAdded_stylesXmlShouldContainNewFormat() throws IOException {
        FollowUpTemplate template = new FollowUpTemplate(resolve(PATH_FOLLOWUP_TEMPLATE));
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(template)) {
            subject.open();

            // given
            String dateFormat = "yyyy/mm/dd\\ hh:mm:ss";
            String currencyFormat = "[$R$-416]\\ #,##0.00;[RED]\\-[$R$-416]\\ #,##0.00";

            // when
            subject.getStylesEditor().getOrCreateNumberFormat(dateFormat);
            subject.save();
            subject.getStylesEditor().getOrCreateNumberFormat(currencyFormat);
            subject.save();

            // then
            NodeList formatList = XmlUtils.xpath(new File(subject.getExtractedSheetDirectory(), subject.getStylesEditor().getPathStyles()), "//numFmts/numFmt");
            assertThat(Streams.stream(XmlUtils.iterable(formatList))
                    .filter(node -> node instanceof Element)
                    .map(node -> (Element)node)
                    .filter(element -> dateFormat.equals(element.getAttribute("formatCode")))
                    .count(), is(1L));

            assertThat(Streams.stream(XmlUtils.iterable(formatList))
                    .filter(node -> node instanceof Element)
                    .map(node -> (Element)node)
                    .filter(element -> currencyFormat.equals(element.getAttribute("formatCode")))
                    .count(), is(1L));
        }
    }
    
    @Test
    public void getTableFromSheet() {
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            subject.open();
            
            SimpleSheet tShirtSizeSheet = subject.getOrCreateSheet("T-shirt Size");
            Optional<SheetTable> clustersTable = tShirtSizeSheet.getTable("Clusters");
            
            assertTrue(clustersTable.isPresent());
            assertEquals("A", clustersTable.get().getReference().getStart().getColumnLetter());
            assertEquals(  1, clustersTable.get().getReference().getStart().getRowNumber());
            assertEquals("E", clustersTable.get().getReference().getEnd().getColumnLetter());
            assertEquals(101, clustersTable.get().getReference().getEnd().getRowNumber());
        }
    }
    
    @Test
    public void getTableFromSheet_nonexistentName() {
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            subject.open();
            
            SimpleSheet tShirtSizeSheet = subject.getOrCreateSheet("T-shirt Size");
            Optional<SheetTable> xTable = tShirtSizeSheet.getTable("Xtable");
            assertFalse(xTable.isPresent());
        }
    }
    
    @Test
    public void getTableFromSheet_sheetWithoutTables() {
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            subject.open();
            
            SimpleSheet activitiesSheet = subject.getOrCreateSheet("Activities");
            Optional<SheetTable> clustersTable = activitiesSheet.getTable("Clusters");
            assertFalse(clustersTable.isPresent());
        }
    }

    private Pair<String, String> expectedAttributeValue(String a, String b) {
        return new ImmutablePair<>(a, b);
    }

    @SafeVarargs
    private final void checkXmlElementInSpreadsheetFile(SimpleSpreadsheetEditor subject, 
            String xmlPath,
            String locator,
            Pair<String, String> ...attributeExpectations) throws IOException {
        String workbookRelContent = FileUtils.readFileToString(new File(subject.getExtractedSheetDirectory(), xmlPath),"UTF-8");
        NodeList list = XmlUtils.xpath(workbookRelContent, locator);
        assertEquals(1, list.getLength());
        for (Pair<String, String> pair : attributeExpectations)
            assertEquals(pair.getValue(), list.item(0).getAttributes().getNamedItem(pair.getKey()).getNodeValue());

    }

    private FollowUpTemplate getBasicTemplate() {
        return new FollowUpTemplate(resolve(PATH_FOLLOWUP_TEMPLATE));
    }

    private static Resource resolve(String resourceName) {
        return IOUtilities.asResource(SpreadsheetEditorTest.class.getClassLoader().getResource(resourceName));
    }

    private void addRow(Sheet sheet, FromJiraDataRow followUpData) {
        SheetRow row = sheet.createRow();

        row.addColumn(followUpData.project);
        row.addColumn(followUpData.demandType);
        row.addColumn(followUpData.demandStatus);
        row.addColumn(followUpData.demandNum);
        row.addColumn(followUpData.demandSummary);
        row.addColumn(followUpData.demandDescription);
        row.addColumn(followUpData.taskType);
        row.addColumn(followUpData.taskStatus);
        row.addColumn(followUpData.taskNum);
        row.addColumn(followUpData.taskSummary);
        row.addColumn(followUpData.taskDescription);
        row.addColumn(followUpData.taskFullDescription);
        row.addColumn(followUpData.subtaskType);
        row.addColumn(followUpData.subtaskStatus);
        row.addColumn(followUpData.subtaskNum);
        row.addColumn(followUpData.subtaskSummary);
        row.addColumn(followUpData.subtaskDescription);
        row.addColumn(followUpData.subtaskFullDescription);
        row.addColumn(followUpData.demandId);
        row.addColumn(followUpData.taskId);
        row.addColumn(followUpData.subtaskId);
        row.addColumn(followUpData.planningType);
        row.addColumn(followUpData.taskRelease);
        row.addColumn(followUpData.worklog);
        row.addColumn(followUpData.wrongWorklog);
        row.addColumn(followUpData.demandBallpark);
        row.addColumn(followUpData.taskBallpark);
        row.addColumn(followUpData.tshirtSize);
        row.addColumn(followUpData.queryType);
        row.addFormula("SUMIFS(Clusters[Effort],Clusters[Cluster Name],AllIssues[[#This Row],[SUBTASK_TYPE]],Clusters[T-Shirt Size],AllIssues[tshirt_size])");
        row.addFormula("SUMIFS(Clusters[Cycle],Clusters[Cluster Name],AllIssues[[#This Row],[SUBTASK_TYPE]],Clusters[T-Shirt Size],AllIssues[tshirt_size])");
        row.addFormula("AllIssues[EffortEstimate]-AllIssues[EffortDone]");
        row.addFormula("AllIssues[CycleEstimate]-AllIssues[CycleDone]");
        row.addFormula("IF(AllIssues[[#This Row],[planning_type]]=\"Ballpark\",AllIssues[EffortEstimate],0)");
        row.addFormula("IF(AllIssues[[#This Row],[planning_type]]=\"Plan\",AllIssues[EffortEstimate],0)");
        row.addFormula("IF(OR(AllIssues[SUBTASK_STATUS]=\"Done\",AllIssues[SUBTASK_STATUS]=\"Cancelled\"),AllIssues[EffortEstimate],0)");
        row.addFormula("IF(OR(AllIssues[SUBTASK_STATUS]=\"Done\",AllIssues[SUBTASK_STATUS]=\"Cancelled\"),AllIssues[CycleEstimate],0)");
        row.addFormula("IF(OR(AllIssues[SUBTASK_STATUS]=\"Done\",AllIssues[SUBTASK_STATUS]=\"Cancelled\"),AllIssues[worklog],0)");
        row.addFormula("IF(OR(AllIssues[SUBTASK_STATUS]=\"Done\",AllIssues[SUBTASK_STATUS]=\"Cancelled\"),0, AllIssues[worklog])");
        row.addFormula("IF(COUNTIFS(AllIssues[TASK_ID],AllIssues[TASK_ID],AllIssues[TASK_ID],\">0\")=0,0,1/COUNTIFS(AllIssues[TASK_ID],AllIssues[TASK_ID],AllIssues[TASK_ID],\">0\"))");
        row.addFormula("IF(COUNTIFS(AllIssues[demand_description],AllIssues[demand_description])=0,0,1/COUNTIFS(AllIssues[demand_description],AllIssues[demand_description]))");
        row.addFormula("IF(AllIssues[planning_type]=\"Plan\",1,0)");
        row.addFormula("IF(AllIssues[[#This Row],[SUBTASK_STATUS]]=\"Done\", AllIssues[[#This Row],[EffortDone]],0)");
        row.addFormula("IF(AllIssues[TASK_TYPE]=\"Bug\",AllIssues[EffortEstimate], 0)");
        row.addFormula("IF(AllIssues[TASK_TYPE]=\"Bug\",AllIssues[worklog],0)");
        row.addColumn(1L);
        row.addColumn(2);
        row.addColumn(3D);
        row.addColumn(LocalDate.parse("2017-01-01").atStartOfDay());
    }
}