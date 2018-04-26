package objective.taskboard.spreadsheet;

import static objective.taskboard.google.SpreadsheetUtils.columnIndexToLetter;
import static objective.taskboard.google.SpreadsheetUtils.columnLetterToIndex;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import objective.taskboard.followup.FollowUpTemplate;
import objective.taskboard.google.SpreadsheetUtils.SpreadsheetA1Range;

public class SimpleSpreadsheetEditorMock implements SpreadsheetEditor {

    private Map<String,String> sheetPathByName = new LinkedHashMap<>();
    private Map<String, SheetTable> tables = new HashMap<>();
    FollowUpTemplate template;

    private StringBuilder logger = new StringBuilder();

    public SimpleSpreadsheetEditorMock() {
    }

    public void open() {
        logger.append("Spreadsheet Open\n");
    }

    public Sheet getSheet(String sheetName) {
        return new SheetMock(sheetName);
    }

    public String loggerString() {
        return logger.toString();
    }
    
    public void addTable(String sheetName, String tableName, SpreadsheetA1Range reference) {
        tables.put(sheetName + ":" + tableName, new SheetTableMock(tableName, reference));
    }

    @Override
    public Sheet createSheet(String sheetName) {
        Optional<Integer> max = sheetPathByName.values().stream()
                .filter(v->v.startsWith("xl/worksheets/sheet"))
                .map(v->Integer.parseInt(v.replaceAll("xl/worksheets/sheet([0-9]+).xml", "$1")))
                .sorted()
                .max(Integer::compareTo);
        int sheetFileNumber = max.orElse(0)+1;

        String sheetPath = "xl/worksheets/sheet"+sheetFileNumber+".xml";

        sheetPathByName.put(sheetName, sheetPath);

        return new SheetMock(sheetName);
    }

    @Override
    public Sheet getOrCreateSheet(String sheetName) {
        if(sheetPathByName.containsKey(sheetName)) {
            return getSheet(sheetName);
        } else {
            return createSheet(sheetName);
        }
    }

    @Override
    public byte[] toBytes() {
        return null;
    }

    @Override
    public void save() throws IOException {
        logger.append("Spreadsheet Save\n");
    }

    @Override
    public void close() throws IOException {
        logger.append("Spreadsheet Close\n");
    }

    @Override
    public SpreadsheetStylesEditor getStylesEditor() {
        return null;
    }

    @Override
    public Map<String, Long> getSharedStrings() {
        return null;
    }

    @Override
    public String generateSharedStrings() throws IOException {
        return null;
    }

    @Override
    public File getExtractedSheetDirectory() {
        return null;
    }

    public class SheetMock implements Sheet {
        private int rowNumber = 0;
        private String sheetName;

        public SheetMock(String sheetName) {
            logger.append("Sheet Create: " + sheetName + "\n");
            this.sheetName = sheetName;
        }

        @Override
        public void save() {
            logger.append("Sheet \"" + sheetName + "\" Save\n");
        }

        @Override
        public SheetRowMock createRow() {
            rowNumber++;
            logger.append("Sheet \"" + sheetName + "\" Row Create: "+ rowNumber +"\n");
            return new SheetRowMock(sheetName, rowNumber);
        }

        @Override
        public SheetRow getOrCreateRow(int rowNumber) {
            this.rowNumber = Math.max(this.rowNumber, rowNumber);
            logger.append("Sheet \"" + sheetName + "\" Row Get/Create: "+ rowNumber +"\n");
            return new SheetRowMock(sheetName, rowNumber);
        }

        @Override
        public void truncate() {
        }

        @Override
        public String getSheetPath() {
            return null;
        }

        @Override
        public Optional<SheetTable> getTable(String tableName) {
            return Optional.ofNullable(tables.get(sheetName + ":" + tableName));
        }
    }

    public class SheetRowMock implements SheetRow {
        private String sheetname;
        private int rowNum;
        private int columIndex = 0;

        public SheetRowMock(String sheetname, int rowNum) {
            this.sheetname = sheetname;
            this.rowNum = rowNum;
        }

        @Override
        public void addColumn(String value) {
            logger.append("Sheet \"" + sheetname + "\" Row \""+ rowNum +"\" AddColumn \""+ columnIndexToLetter(columIndex) + rowNum +"\": " + value + "\n");
            columIndex++;
        }

        @Override
        public void addColumn(Number value) {
            logger.append("Sheet \"" + sheetname + "\" Row \""+ rowNum +"\" AddColumn \""+ columnIndexToLetter(columIndex) + rowNum +"\": " + value + "\n");
            columIndex++;
        }

        @Override
        public void addColumn(Boolean value) {
            logger.append("Sheet \"" + sheetname + "\" Row \""+ rowNum +"\" AddColumn \""+ columnIndexToLetter(columIndex) + rowNum +"\": " + String.valueOf(value) + "\n");
            columIndex++;
        }

        @Override
        public void addColumn(ZonedDateTime value) {
            logger.append("Sheet \"" + sheetname + "\" Row \""+ rowNum +"\" AddColumn \""+ columnIndexToLetter(columIndex) + rowNum +"\": " + value + "\n");
            columIndex++;
        }
        
        @Override
        public void addColumn(LocalDateTime value) {
            logger.append("Sheet \"" + sheetname + "\" Row \""+ rowNum +"\" AddColumn \""+ columnIndexToLetter(columIndex) + rowNum +"\": " + value + "\n");
            columIndex++;
        }

        @Override
        public void addColumn(LocalDate value) {
            logger.append("Sheet \"" + sheetname + "\" Row \""+ rowNum +"\" AddColumn \""+ columnIndexToLetter(columIndex) + rowNum +"\": " + value + "\n");
            columIndex++;
        }

        @Override
        public void addFormula(String formula) {
            logger.append("Sheet \"" + sheetname + "\" Row \""+ rowNum +"\" AddFormula \""+ columnIndexToLetter(columIndex) + rowNum +"\": " + formula + "\n");
            columIndex++;
        }
        
        @Override
        public void setValue(String columnLetter, String value) {
            internalSetValue(columnLetter, value, "string");
        }

        @Override
        public void setValue(String columnLetter, Number value) {
            internalSetValue(columnLetter, value, "number");
        }

        @Override
        public void setValue(String columnLetter, Boolean value) {
            internalSetValue(columnLetter, value, "boolean");
        }

        @Override
        public void setValue(String columnLetter, LocalDateTime value) {
            internalSetValue(columnLetter, value, "date-time");
        }

        @Override
        public void setValue(String columnLetter, LocalDate value) {
            internalSetValue(columnLetter, value, "date");
        }

        @Override
        public void setFormula(String columnLetter, String value) {
            internalSetValue(columnLetter, value, "formula");
        }

        private void internalSetValue(String columnLetter, Object value, String type) {
            logger.append(String.format("Sheet \"%s\" Row \"%s\" SetValue (%s) \"%s\": %s\n", sheetname, rowNum, type, columnLetter + rowNum, value));
            columIndex = Math.max(columIndex, columnLetterToIndex(columnLetter));
        }
    }

    protected class SimpleSpreadsheetStylesEditorMock implements SpreadsheetStylesEditor {
        protected static final String PATH_STYLES = "xl/styles.xml";

        @Override
        public int getOrCreateNumberFormat(String format) {
            return 0;
        }

        @Override
        public void save() {
        }

        @Override
        public String getPathStyles() {
            return null;
        }
    }

    private static class SheetTableMock implements SheetTable {
        private final String name;
        private final SpreadsheetA1Range reference;
        
        public SheetTableMock(String name, SpreadsheetA1Range reference) {
            this.name = name;
            this.reference = reference;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public SpreadsheetA1Range getReference() {
            return reference;
        }
    }
}
