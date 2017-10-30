package objective.taskboard.spreadsheet;

import static objective.taskboard.utils.IOUtilities.resourceToString;
import static objective.taskboard.utils.XmlUtils.normalizeXml;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import objective.taskboard.followup.FollowUpHelper;
import objective.taskboard.followup.FollowUpTemplate;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor.Sheet;
import objective.taskboard.utils.XmlUtils;

public class SimpleSpreadsheetEditorTest extends AbstractEditorTest {

    @Test
    public void addOneRowToSpreadSheet() {
        FollowUpTemplate template = getBasicTemplate();
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(template)) {
            subject.open();
            
            Sheet sheet = subject.getSheet("From Jira");
            sheet.truncate(1);
            addRow(sheet, FollowUpHelper.getDefaultFromJiraDataRow());
            sheet.save();
    
            String jiraDataSheet = subject.getSheet("From Jira").stringValue();
            String jiraDataSheetExpected = normalizeXml(resourceToString("followup/fromJiraWithOneRow.xml"));
            assertEquals("Jira data sheet", jiraDataSheetExpected, jiraDataSheet);        
        }
    }
    
    @Test
    public void truncateFromJira() throws ParserConfigurationException, SAXException, IOException {
        FollowUpTemplate template = getBasicTemplate();
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(template)) {
            subject.open();
            
            Sheet sheet = subject.getSheet("From Jira");
            sheet.truncate(1);
            String jiraDataSheet = sheet.stringValue();
    
            String jiraDataSheetExpected = normalizeXml(resourceToString("followup/emptyFromJiraOnlyWithHeaders.xml"));
            
            assertEquals("Jira data sheet", jiraDataSheetExpected, jiraDataSheet);
        }
    }
    
    @Test
    public void getSheetByName_shouldFindTheSheetPart() {
        try(SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            subject.open();
            Sheet sheet = subject.getSheet("T-shirt Size");
            String path = sheet.getSheetPath();
            
            assertEquals("xl/worksheets/sheet6.xml", path);
        }
    }

    @Test
    public void whenNewSheetIsAdded_updateExpectedFiles() throws Exception {
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            subject.open();
            
            Sheet sheet = subject.createSheet("A New Sheet");
            assertEquals("xl/worksheets/sheet10.xml", sheet.getSheetPath());
            
            Sheet sheetAgain = subject.getSheet("A New Sheet");
            
            assertNotNull(sheetAgain);

            subject.save();
            
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
    public void generateLotsOfLines() throws IOException {
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(getBasicTemplate())) {
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
        try (SimpleSpreadsheetEditor subject = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            subject.open();
            subject.save();
            
            NodeList calcPrList = XmlUtils.xpath(new File(subject.getExtractedSheetDirectory(), "xl/workbook.xml"), "/workbook/calcPr");
            Node calcPr = calcPrList.item(0);
            Node fullCalcOnLoad = calcPr.getAttributes().getNamedItem("fullCalcOnLoad");
            
            assertEquals("1", fullCalcOnLoad.getNodeValue());
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
}