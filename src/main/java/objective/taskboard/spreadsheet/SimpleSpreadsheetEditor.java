package objective.taskboard.spreadsheet;

import static objective.taskboard.google.SpreadsheetUtils.columnIndexToLetter;
import static objective.taskboard.utils.DateTimeUtils.toDoubleExcelFormat;
import static objective.taskboard.utils.IOUtilities.write;
import static objective.taskboard.utils.ZipUtils.unzip;
import static objective.taskboard.utils.ZipUtils.zip;
import static org.apache.commons.lang.ObjectUtils.defaultIfNull;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
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
    protected final SpreadsheetStylesEditor stylesEditor = new SpreadsheetStylesEditor();
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

    protected class SpreadsheetStylesEditor {
        protected static final String PATH_STYLES = "xl/styles.xml";
        private static final int FIRST_numFmtId = 164;

        private boolean loaded = false;
        private boolean modified = false;
        private BiMap<Integer, String> numberFormat;
        private int initialNumberFormatCount;

        private List<String> styleIndexes;
        private Multimap<Integer, Integer> styleMap;
        private int initialStylesCount;

        public int getOrCreateNumberFormat(String format) {
            ensureLoaded();

            Integer key = numberFormat.inverse().get(format);
            if(key == null)
                key = createNumberFormat(format);

            if(styleMap.containsKey(key)) {
                return Iterables.getFirst(styleMap.get(key), null);
            } else {
                return createStyleIndex(key);
            }
        }

        public void save() {
            if(modified) {
                File fileSharedStrings = new File(extractedSheetDirectory, PATH_STYLES);
                write(fileSharedStrings, generateStyles());
                loaded = false;
            }
        }

        private void ensureLoaded() {
            if(!loaded) {
                numberFormat = loadInternalFormats();
                styleIndexes = new ArrayList<>();
                styleMap = HashMultimap.create();

                try (InputStream inputStream = new FileInputStream(new File(extractedSheetDirectory, PATH_STYLES))) {
                    Document doc = XmlUtils.asDocument(inputStream);

                    NodeList numFmts = XmlUtils.xpath(doc, "//numFmts/numFmt");
                    for (Node node : XmlUtils.iterable(numFmts)) {
                        Element numFmt = (Element) node;

                        numberFormat.put(Integer.valueOf(numFmt.getAttribute("numFmtId")), numFmt.getAttribute("formatCode"));
                    }
                    initialNumberFormatCount = numberFormat.size();

                    NodeList xfs = XmlUtils.xpath(doc, "//cellXfs/xf");
                    for(int index = 0; index < xfs.getLength(); ++index) {
                        Element xf = (Element) xfs.item(index);

                        String id = xf.getAttribute("numFmtId");
                        styleIndexes.add(id);
                        styleMap.put(Integer.parseInt(id), index);
                    }
                    initialStylesCount = styleIndexes.size();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                loaded = true;
            }
        }

        private BiMap<Integer, String> loadInternalFormats() {
            BiMap<Integer, String> numberFormat = HashBiMap.create();

            // source:
            // http://www.ecma-international.org/news/TC45_current_work/Office%20Open%20XML%20Part%204%20-%20Markup%20Language%20Reference.pdf
            // page 2128
            numberFormat.put(0, "General");
            numberFormat.put(1, "0");
            numberFormat.put(2, "0.00");
            numberFormat.put(3, "#,##0");
            numberFormat.put(4, "#,##0.00");
            numberFormat.put(9, "0%");
            numberFormat.put(10, "0.00%");
            numberFormat.put(11, "0.00E+00");
            numberFormat.put(12, "# ?/?");
            numberFormat.put(13, "# ??/??");
            numberFormat.put(14, "mm-dd-yy");
            numberFormat.put(15, "d-mmm-yy");
            numberFormat.put(16, "d-mmm");
            numberFormat.put(17, "mmm-yy");
            numberFormat.put(18, "h:mm AM/PM");
            numberFormat.put(19, "h:mm:ss AM/PM");
            numberFormat.put(20, "h:mm");
            numberFormat.put(21, "h:mm:ss");
            numberFormat.put(22, "m/d/yy h:mm");
            numberFormat.put(37, "#,##0 ;(#,##0)");
            numberFormat.put(38, "#,##0 ;[Red](#,##0)");
            numberFormat.put(39, "#,##0.00;(#,##0.00)");
            numberFormat.put(40, "#,##0.00;[Red](#,##0.00)");
            numberFormat.put(45, "mm:ss");
            numberFormat.put(46, "[h]:mm:ss");
            numberFormat.put(47, "mmss.0");
            numberFormat.put(48, "##0.0E+0");
            numberFormat.put(49, "@");

            return numberFormat;
        }

        private Integer createNumberFormat(String format) {
            Optional<Integer> max = numberFormat.keySet()
                    .stream()
                    .max(Integer::compareTo);
            Integer key = max.isPresent() ? max.get() + 1 : FIRST_numFmtId;
            numberFormat.put(key, format);
            modified = true;
            return key;
        }

        private Integer createStyleIndex(Integer key) {
            String keyString = key.toString();
            int index = styleIndexes.size();
            styleIndexes.add(keyString);
            styleMap.put(key, index);
            modified = true;
            return index;
        }

        private String generateStyles() {
            try (InputStream inputStream = new FileInputStream(new File(extractedSheetDirectory, PATH_STYLES))) {
                Document doc = XmlUtils.asDocument(inputStream);

                Node numFmtsNode = doc.getElementsByTagName("numFmts").item(0);
                if(numFmtsNode == null) {
                    numFmtsNode = doc.createElement("numFmts");
                    doc.getDocumentElement().appendChild(numFmtsNode);
                }
                Element numFmts = (Element) numFmtsNode;
                numberFormat.entrySet().stream()
                        .skip(initialNumberFormatCount)
                        .forEach(entry -> {
                            Element numFmt = doc.createElement("numFmt");
                            numFmt.setAttribute("numFmtId", entry.getKey().toString());
                            numFmt.setAttribute("formatCode", entry.getValue());
                            numFmts.appendChild(numFmt);
                        });
                numFmts.setAttribute("count", Integer.toString(numberFormat.size()));

                Element cellXfs = (Element) doc.getElementsByTagName("cellXfs").item(0);
                styleIndexes.stream().skip(initialStylesCount)
                        .forEach(id -> {
                            Element xf = doc.createElement("xf");
                            xf.setAttribute("numFmtId", id);
                            xf.setAttribute("fontId", "0");
                            xf.setAttribute("fillId", "0");
                            xf.setAttribute("borderId", "0");
                            xf.setAttribute("xfId", "0");
                            cellXfs.appendChild(xf);
                        });
                cellXfs.setAttribute("count", Integer.toString(styleIndexes.size()));

                return XmlUtils.asString(doc);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

}