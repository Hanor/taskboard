package objective.taskboard.spreadsheet;

import static objective.taskboard.google.SpreadsheetUtils.columnIndexToLetter;
import static objective.taskboard.utils.DateTimeUtils.toDoubleExcelFormat;
import static objective.taskboard.utils.ZipUtils.unzip;
import static objective.taskboard.utils.ZipUtils.zip;
import static org.apache.commons.lang.ObjectUtils.defaultIfNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import objective.taskboard.followup.FollowUpTemplate;
import objective.taskboard.utils.IOUtilities;
import objective.taskboard.utils.XmlUtils;

public class SimpleSpreadsheetEditor implements Closeable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SimpleSpreadsheetEditor.class);

    FollowUpTemplate template;
    protected File extractedSheetDirectory;
    protected final WorkbookEditor workbookEditor = new WorkbookEditor(this);
    protected final SharedStringsEditor sharedStringsEditor = new SharedStringsEditor(this);
    protected final SpreadsheetStylesEditor stylesEditor = new SpreadsheetStylesEditor(this);
    protected final ContentTypeEditor contentTypeEditor = new ContentTypeEditor(this);

    public SimpleSpreadsheetEditor(FollowUpTemplate template) {
        this.template = template;
    }
    
    public void open() {
        extractedSheetDirectory = decompressTemplate().toFile();
    }

    public Sheet getSheet(String sheetName) {
        return workbookEditor.getSheet(sheetName);
    }

    public Sheet createSheet(String sheetName) {
        return workbookEditor.createSheet(sheetName);
    }

    public Sheet getOrCreateSheet(String sheetName) {
        return workbookEditor.getOrCreateSheet(sheetName);
    }

    public byte[] toBytes() {
        Path pathFollowupXLSM = null;
        try {
            save();
            pathFollowupXLSM = compress(extractedSheetDirectory.toPath());
            
            return Files.readAllBytes(pathFollowupXLSM);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        finally {
            if (pathFollowupXLSM != null && pathFollowupXLSM.toFile().exists())
                try {
                    Files.delete(pathFollowupXLSM);
                } catch (IOException e) {
                    log.warn("Could not remove " + pathFollowupXLSM.toString(), e);
                }
        }
    }

    @Override
    public void close() {
        if (extractedSheetDirectory == null || !extractedSheetDirectory.exists()) return;
        
        try {
            FileUtils.deleteDirectory(extractedSheetDirectory);
        } catch (IOException e) {
            log.warn("Could not remove " + extractedSheetDirectory.toString(), e);
        }
    }
    
    File getExtractedSheetDirectory() {
        return extractedSheetDirectory;
    }
    

    private Path decompressTemplate() {
        Path pathFollowup;
        try {
            pathFollowup = Files.createTempDirectory("Followup");
            unzip(template.getPathFollowupTemplateXLSM().getInputStream(), pathFollowup);
            return pathFollowup;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void save() throws IOException {
        sharedStringsEditor.save();
        workbookEditor.save();
        resetCalcChainToAvoidFormulaCorruption();
        stylesEditor.save();
        contentTypeEditor.save();
    }
    
    private void resetCalcChainToAvoidFormulaCorruption() {
        new File(extractedSheetDirectory, "xl/calcChain.xml").delete();
    }
    
    private Path compress(Path directoryFollowup) throws IOException  {
        Path pathFollowupXLSM = Files.createTempFile("Followup", ".xlsm");
        zip(directoryFollowup, pathFollowupXLSM);
        return pathFollowupXLSM;
    }
    
    public class Sheet {
        private File sheetFile;
        int maxCol = 0;
        private Document sheetDoc;
        private Node sheetData;
        int rowCount;

        public Sheet(String sheetPath) {
            sheetFile = new File(extractedSheetDirectory, sheetPath);
            sheetDoc = XmlUtils.asDocument(sheetFile);
            NodeList sheetDataTags = sheetDoc.getElementsByTagName("sheetData");
            if (sheetDataTags.getLength() == 0)
                throw new IllegalArgumentException("Malformed sheet part found. Missing sheetData");
            sheetData = sheetDataTags.item(0);
            rowCount = sheetDoc.getElementsByTagName("row").getLength();
        }

        public void save() {
            IOUtilities.write(sheetFile, XmlUtils.asString(sheetDoc));
        }

        public SheetRow createRow() {
            int nextRowNumber = getRowCount() + 1;
            return new SheetRow(this, nextRowNumber, sheetDoc);
        }

        private int getRowCount() {
            return rowCount;
        }

        public void truncate(int starting) {
            NodeList xmlRow = sheetDoc.getElementsByTagName("row");

            List<Node> rowsToKeep = new LinkedList<>();
            int rows = Math.min(starting, xmlRow.getLength());
            for (int i = 0; i < rows; i++)
                rowsToKeep.add(xmlRow.item(i));

            while(sheetData.hasChildNodes())
                sheetData.removeChild(sheetData.getFirstChild());

            rowsToKeep.stream().forEach(node->sheetData.appendChild(node));
            rowCount = rows;
        }

        public String stringValue() {
            return XmlUtils.asString(sheetDoc);
        }

        public String getSheetPath() {
            return sheetFile.getPath().replace(extractedSheetDirectory.getPath()+File.separator, "");
        }

        private void addRow(SheetRow r) {
            if (r.getColumnIndex() > maxCol)
                maxCol = r.getColumnIndex();
            sheetData.appendChild(r.buildNode());
            rowCount++;
        }
    }

    public class SheetRow {
        private int rowNumber;
        private int columnIndex = 0;
        private Sheet sheet;
        private Document sheetDoc;
        private Element row;

        public SheetRow(Sheet sheet, int rowNumber, Document sheetDoc) {
            this.sheet = sheet;
            this.rowNumber = rowNumber;
            this.sheetDoc = sheetDoc;
            this.row = sheetDoc.createElement("row");
        }

        public Node buildNode() {
            row.setAttribute("r", Integer.toString(rowNumber));
            row.setAttribute("spans", "1:"+columnIndex);
            row.setAttribute("x14ac:dyDescent", "0.25");

            return row;
        }

        public void addColumn(String value) {
            Element column = addColumn(sharedStringsEditor.getOrSetIndexInSharedStrings(value), "v");
            column.setAttribute("t", "s");
        }

        public void addColumn(Number value) {
            addColumn(defaultIfNull(value, "").toString(), "v");
        }

        public void addColumn(ZonedDateTime value) {
            Element column = addColumn(toDoubleExcelFormat(value, workbookEditor.isDate1904()), "v");
            column.setAttribute("s", Integer.toString(
                    stylesEditor.getOrCreateNumberFormat("m/d/yy h:mm")));
        }

        public void addFormula(String formula) {
            Element column = addColumn(formula, "f");
            column.setAttribute("s", "4");
        }

        private Element addColumn(String colVal, String tagName) {
            Element column = sheetDoc.createElement("c");
            column.setAttribute("r", columnLabel());
            Element valueNode = sheetDoc.createElement(tagName);
            valueNode.appendChild(sheetDoc.createTextNode(colVal));
            column.appendChild(valueNode);
            row.appendChild(column);
            columnIndex++;
            return column;
        }

        private String columnLabel() {
            return columnIndexToLetter(columnIndex)+rowNumber;
        }

        public int getColumnIndex() {
            return columnIndex;
        }

        public void save() {
            sheet.addRow(this);
        }
    }
}