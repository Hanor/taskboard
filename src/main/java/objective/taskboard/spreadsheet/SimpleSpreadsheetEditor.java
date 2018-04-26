package objective.taskboard.spreadsheet;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static objective.taskboard.google.SpreadsheetUtils.columnIndexToLetter;
import static objective.taskboard.google.SpreadsheetUtils.columnLetterToIndex;
import static objective.taskboard.utils.IOUtilities.write;
import static objective.taskboard.utils.XmlUtils.getAttributeValue;
import static objective.taskboard.utils.XmlUtils.getAttributeValueOrCry;
import static objective.taskboard.utils.ZipUtils.unzip;
import static objective.taskboard.utils.ZipUtils.zip;
import static org.apache.commons.lang3.StringUtils.join;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import objective.taskboard.followup.FollowUpTemplate;
import objective.taskboard.google.SpreadsheetUtils.SpreadsheetA1;
import objective.taskboard.google.SpreadsheetUtils.SpreadsheetA1Range;
import objective.taskboard.utils.IOUtilities;
import objective.taskboard.utils.XmlUtils;

public class SimpleSpreadsheetEditor implements SpreadsheetEditor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SimpleSpreadsheetEditor.class);
    private static final String TAG_T_IN_SHARED_STRINGS = "t";
    private static final String PATH_SHARED_STRINGS = "xl/sharedStrings.xml";

    public Map<String, Long> sharedStrings;
    FollowUpTemplate template;
    private File extractedSheetDirectory;
    private Map<String,String> sheetPathByName = new LinkedHashMap<>();
    private int initialSharedStringCount;
    private WorkbookEditor workbookEditor = new WorkbookEditor();
    private SpreadsheetStylesEditor stylesEditor = new SimpleSpreadsheetStylesEditor();

    @Override
    public SpreadsheetStylesEditor getStylesEditor() {
        return this.stylesEditor;
    }

    public SimpleSpreadsheetEditor(FollowUpTemplate template) {
        this.template = template;
    }

    @Override
    public void open() {
        extractedSheetDirectory = decompressTemplate().toFile();
        sharedStrings = initializeSharedStrings();
        initializeWorkbookRelations();
    }

    @Override
    public SimpleSheet getSheet(String sheetName) {
        return new SimpleSheet(sheetPathByName.get(sheetName));
    }

    @Override
    public SimpleSheet createSheet(String sheetName) {
        Optional<Integer> max = sheetPathByName.values().stream()
            .filter(v->v.startsWith("xl/worksheets/sheet"))
            .map(v->Integer.parseInt(v.replaceAll("xl/worksheets/sheet([0-9]+).xml", "$1")))
            .sorted()
            .max(Integer::compareTo);
        int sheetFileNumber = max.orElse(0)+1;

        String sheetPath = "xl/worksheets/sheet"+sheetFileNumber+".xml";
        createEmptySheet(sheetPath);

        sheetPathByName.put(sheetName, sheetPath);
        String relId = addWorkbookRel(sheetFileNumber);
        addWorkbook(sheetName, relId);
        addContentTypeOverride(sheetPath);

        return new SimpleSheet(sheetPath);
    }

    @Override
    public SimpleSheet getOrCreateSheet(String sheetName) {
        if(sheetPathByName.containsKey(sheetName)) {
            return getSheet(sheetName);
        } else {
            return createSheet(sheetName);
        }
    }

    private String addWorkbookRel(int next) {
        Document workbookRelsDoc = getWorkbookRelsDoc();

        Element relationship = workbookRelsDoc.createElement("Relationship");
        String relationId = "rId"+computeNextRelationId(workbookRelsDoc);
        relationship.setAttribute("Id", relationId);
        relationship.setAttribute("Target", "worksheets/sheet" + next + ".xml");
        relationship.setAttribute("Type", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet");
        workbookRelsDoc.getElementsByTagName("Relationships").item(0).appendChild(relationship);
        IOUtilities.write(getWorkbookRelsFile(), XmlUtils.asString(workbookRelsDoc));

        return relationId;
    }

    private void addWorkbook(String sheetName, String relationId) {
        Document workbookDoc = getWorkbook();

        Element sheetEl = workbookDoc.createElement("sheet");
        sheetEl.setAttribute("name", sheetName);
        sheetEl.setAttribute("r:id", relationId);
        sheetEl.setAttribute("sheetId", Integer.toString(computeNextSheetId(workbookDoc)));
        workbookDoc.getElementsByTagName("sheets").item(0).appendChild(sheetEl);
        IOUtilities.write(getWorkbookFile(), XmlUtils.asString(workbookDoc));
    }

    @Override
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

    public File getExtractedSheetDirectory() {
        return extractedSheetDirectory;
    }

    private int computeNextRelationId(Document workbookRelsDoc) {
        final NodeList relationShips = workbookRelsDoc.getElementsByTagName("Relationship");
        int nextRelationId = 0;
        for(int i = 0; i < relationShips.getLength(); i++) {
            Node item = relationShips.item(i);
            int id = Integer.parseInt(item.getAttributes().getNamedItem("Id").getNodeValue().replaceAll("[^0-9]", ""));
            if (id > nextRelationId)
                nextRelationId = id;
        }
        nextRelationId++;
        return nextRelationId;
    }

    private int computeNextSheetId(Document workbookDoc) {
        final NodeList sheet = workbookDoc.getElementsByTagName("sheet");

        int sheetId = 0;
        for(int i = 0; i < sheet.getLength(); i++) {
            Node item = sheet.item(i);
            Node sheetIdAttr = item.getAttributes().getNamedItem("sheetId");
            if (sheetIdAttr == null) continue;

            int id = Integer.parseInt(sheetIdAttr.getNodeValue().replaceAll("[^0-9]", ""));
            if (id > sheetId)
                sheetId = id;
        }
        sheetId++;
        return sheetId;
    }

    private void addContentTypeOverride(String sheetPath) {
        File contentTypeOverrideFile = new File(extractedSheetDirectory, "[Content_Types].xml");
        Document contentTypeOverride = XmlUtils.asDocument(contentTypeOverrideFile);
        Node root = contentTypeOverride.getElementsByTagName("Types").item(0);
        Element override = contentTypeOverride.createElement("Override");
        override.setAttribute("ContentType", "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml");
        override.setAttribute("PartName", "/"+sheetPath);
        root.appendChild(override);

        IOUtilities.write(contentTypeOverrideFile, XmlUtils.asString(contentTypeOverride));
    }

    private void createEmptySheet(String sheetPath) {
        try {
            FileUtils.write(new File(extractedSheetDirectory,sheetPath), 
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                    "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                    + "xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\" "
                    + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" "
                    + "xmlns:x14ac=\"http://schemas.microsoft.com/office/spreadsheetml/2009/9/ac\" "
                    + "xmlns:xr=\"http://schemas.microsoft.com/office/spreadsheetml/2014/revision\" "
                    + "xmlns:xr2=\"http://schemas.microsoft.com/office/spreadsheetml/2015/revision2\" "
                    + "xmlns:xr3=\"http://schemas.microsoft.com/office/spreadsheetml/2016/revision3\" "
                    + "mc:Ignorable=\"x14ac xr xr2 xr3\">\n" + 
                    "  <dimension ref=\"A1\"/>\n" + 
                    "  <sheetViews>\n" + 
                    "    <sheetView workbookViewId=\"0\"/>\n" + 
                    "  </sheetViews>\n" + 
                    "  <sheetFormatPr defaultRowHeight=\"15.0\"/>\n" + 
                    "  <sheetData/>\n" + 
                    "  <pageMargins bottom=\"0.75\" footer=\"0.3\" header=\"0.3\" left=\"0.7\" right=\"0.7\" top=\"0.75\"/>\n" + 
                    "</worksheet>", 
                    "UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private InputStream getSharedStringsInputStream() {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(extractedSheetDirectory, PATH_SHARED_STRINGS));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return inputStream;
    }

    @Override
    public Map<String, Long> getSharedStrings() {
        return sharedStrings;
    }

    public String generateSharedStrings() throws IOException {
        Document sharedStringsDoc = XmlUtils.asDocument(getSharedStringsInputStream());
        Node root = sharedStringsDoc.getElementsByTagName("sst").item(0);

        List<String> sharedStringsSorted = sharedStrings.keySet().stream()
            .sorted((s1, s2) -> sharedStrings.get(s1).compareTo(sharedStrings.get(s2)))
            .collect(toList());

        List<String> newStrings = sharedStringsSorted.subList(initialSharedStringCount, sharedStringsSorted.size());
        if (newStrings.size() > 0)
            root.appendChild(sharedStringsDoc.createTextNode("  "));

        for (String sharedString : newStrings) {
            Node si = sharedStringsDoc.createElement("si");
            Node t = sharedStringsDoc.createElement("t");
            si.appendChild(t);
            t.appendChild(sharedStringsDoc.createTextNode(sharedString));
            root.appendChild(si);
        }
        root.getAttributes().getNamedItem("uniqueCount").setNodeValue(Integer.toString(sharedStringsSorted.size()));

        return XmlUtils.asString(sharedStringsDoc);
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

    @Override
    public void save() throws IOException {
        File fileSharedStrings = new File(extractedSheetDirectory, PATH_SHARED_STRINGS);
        write(fileSharedStrings, generateSharedStrings());
        ensureFullCalcOnReloadIsSet();
        resetCalcChainToAvoidFormulaCorruption();
        stylesEditor.save();
    }

    private void ensureFullCalcOnReloadIsSet() {
        Document workbook = getWorkbook();
        Node calcPr = workbook.getElementsByTagName("calcPr").item(0);
        Attr fullCalcOnLoad = workbook.createAttribute("fullCalcOnLoad");
        fullCalcOnLoad.setValue("1");
        calcPr.getAttributes().setNamedItem(fullCalcOnLoad);
        IOUtilities.write(getWorkbookFile(), XmlUtils.asString(workbook));
    }

    private void resetCalcChainToAvoidFormulaCorruption() {
        new File(extractedSheetDirectory, "xl/calcChain.xml").delete();
    }

    private Path compress(Path directoryFollowup) throws IOException  {
        Path pathFollowupXLSM = Files.createTempFile("Followup", ".xlsm");
        zip(directoryFollowup, pathFollowupXLSM);
        return pathFollowupXLSM;
    }

    private void initializeWorkbookRelations() {
        final Document workbookRels = getWorkbookRelsDoc();
        final NodeList relationShips = workbookRels.getElementsByTagName("Relationship");
        final Map<String,String> relations = new LinkedHashMap<>();
        for (int i = 0; i < relationShips.getLength(); i++) {
            Node sheetNode = relationShips.item(i);
            sheetNode.getAttributes().getNamedItem("Id");
            relations.put(sheetNode.getAttributes().getNamedItem("Id").getNodeValue(), 
                    sheetNode.getAttributes().getNamedItem("Target").getNodeValue());
        }
        
        final Document workbook = getWorkbook();

        final NodeList sheetList = workbook.getElementsByTagName("sheet");
        for (int i = 0; i < sheetList.getLength(); i++) {
            Node sheetNode = sheetList.item(i);
            String id = sheetNode.getAttributes().getNamedItem("r:id").getNodeValue();
            String name = sheetNode.getAttributes().getNamedItem("name").getNodeValue();
            sheetPathByName.put(name, "xl/"+relations.get(id));
        }
    }

    private Document getWorkbook() {
        return XmlUtils.asDocument(getWorkbookFile());
    }

    private File getWorkbookFile() {
        return new File(extractedSheetDirectory, "xl/workbook.xml");
    }

    private Document getWorkbookRelsDoc() {
        return XmlUtils.asDocument(getWorkbookRelsFile());
    }

    private File getWorkbookRelsFile() {
        return new File(extractedSheetDirectory, "xl/_rels/workbook.xml.rels");
    }

    private Map<String, Long> initializeSharedStrings() {
        InputStream inputStream = getSharedStringsInputStream();

        Map<String, Long> sharedStrings = new HashMap<>();
        Document doc = XmlUtils.asDocument(inputStream);
        doc.getDocumentElement().normalize();
        NodeList nodes = doc.getElementsByTagName(TAG_T_IN_SHARED_STRINGS);

        for (Long index = 0L; index < nodes.getLength(); index++) {
            Node node = nodes.item(index.intValue());
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            sharedStrings.put(node.getTextContent(), index);
        }
        initialSharedStringCount = sharedStrings.size();
        return sharedStrings;
    }

    public class SimpleSheet implements Sheet {
        private final File sheetFile;
        private final Document sheetDoc;
        private final Element sheetData;
        private Map<String, Document> tablesByName;

        private int lastRowNumber;

        public SimpleSheet(String sheetPath) {
            sheetFile = new File(extractedSheetDirectory, sheetPath);
            sheetDoc = XmlUtils.asDocument(sheetFile);
            sheetData = getSheetData();

            validateReferences();
            
            lastRowNumber = XmlUtils.stream(getRows()).mapToInt(this::getRowNumber).max().orElse(0);
        }

        private Element getSheetData() {
            NodeList sheetDataTags = sheetDoc.getElementsByTagName("sheetData");
            if (sheetDataTags.getLength() == 0)
                throw new IllegalArgumentException("Malformed sheet part found. Missing sheetData");

            return (Element) sheetDataTags.item(0);
        }

        private void validateReferences() {
            List<String> invalidElements = new ArrayList<>();
            
            NodeList rows = getRows();
            for (int rowIndex = 0; rowIndex < rows.getLength(); rowIndex++) {
                Element row = (Element) rows.item(rowIndex);
                
                if (!getAttributeValue(row, "r").isPresent())
                    invalidElements.add("row index " + rowIndex);
                
                NodeList cells = row.getElementsByTagName("c");
                for (int cellIndex = 0; cellIndex < cells.getLength(); cellIndex++) {
                    Element cell = (Element) cells.item(cellIndex);
                    
                    if (!getAttributeValue(cell, "r").isPresent())
                        invalidElements.add("row index " + rowIndex + " > cell index " + cellIndex);
                }
            }
            
            if (!invalidElements.isEmpty())
                throw new IllegalArgumentException("Malformed sheet part found. Elements without reference (attribute 'r'):\n -" + join(invalidElements, "\n -"));
        }

        public String stringValue() {
            return XmlUtils.asString(sheetDoc);
        }

        @Override
        public void save() {
            IOUtilities.write(sheetFile, XmlUtils.asString(sheetDoc));
        }

        @Override
        public SimpleSheetRow createRow() {
            return createRow(lastRowNumber + 1);
        }
        
        private SimpleSheetRow createRow(int rowNumber) {
            Element rowElement = sheetDoc.createElement("row");
            rowElement.setAttribute("r", Integer.toString(rowNumber));
            rowElement.setAttribute("x14ac:dyDescent", "0.25");

            if (rowNumber > lastRowNumber) {
                sheetData.appendChild(rowElement);
                lastRowNumber = rowNumber;
            } else {
                Node nextRowElement = getNextRowElement(rowNumber);
                sheetData.insertBefore(rowElement, nextRowElement);
            }

            return new SimpleSheetRow(rowNumber, rowElement, sheetDoc);
        }

        @Override
        public SheetRow getOrCreateRow(int rowNumber) {
            Optional<Element> existingRowElement = getRowElementByNumber(rowNumber);

            return existingRowElement.isPresent() 
                    ? new SimpleSheetRow(rowNumber, existingRowElement.get(), sheetDoc) 
                    : createRow(rowNumber);
        }

        private NodeList getRows() {
            return sheetData.getElementsByTagName("row");
        }

        private Optional<Element> getRowElementByNumber(int rowNumber) {
            return XmlUtils.stream(getRows())
                    .filter(row -> getRowNumber(row) == rowNumber)
                    .map(row -> (Element) row)
                    .findFirst();
        }
        
        private int getRowNumber(Node row) {
            return Integer.parseInt(getAttributeValueOrCry(row, "r"));
        }

        private Node getNextRowElement(int rowNumber) {
            return XmlUtils.stream(getRows())
                    .filter(row -> getRowNumber(row) > rowNumber)
                    .sorted(comparing(this::getRowNumber))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public void truncate() {
            XmlUtils.removeAllChildren(sheetData);
            lastRowNumber = 0;
        }

        @Override
        public String getSheetPath() {
            return sheetFile.getPath().replace(extractedSheetDirectory.getPath()+File.separator, "");
        }

        private Map<String, Document> getTablesByName() {
            if (tablesByName == null)
                tablesByName = loadTablesByName();

            return tablesByName;
        }

        private Map<String, Document> loadTablesByName() {
            NodeList tableParts = XmlUtils.xpath(sheetDoc, "//tableParts/tablePart");
            if (tableParts.getLength() == 0)
                return Collections.emptyMap();
            
            File relsFile = getRelsFile(sheetFile);
            Document relsDoc = XmlUtils.asDocument(relsFile);
            
            Map<String, String> targetsByRelId = XmlUtils.stream(relsDoc.getElementsByTagName("Relationship"))
                    .collect(toMap(n -> getAttributeValueOrCry(n, "Id"), n -> getAttributeValueOrCry(n, "Target")));
            
            return XmlUtils.stream(tableParts)
                    .map(tablePart -> {
                        String tableId = getAttributeValueOrCry(tablePart, "r:id");
                        String relTarget = targetsByRelId.get(tableId);
                        File tableFile = new File(sheetFile.getParent(), relTarget);
                        return XmlUtils.asDocument(tableFile);
                    })
                    .collect(toMap(d -> getAttributeValueOrCry(d.getDocumentElement(), "name"), Function.identity()));
        }

        @Override
        public Optional<SheetTable> getTable(String tableName) {
            Document tableDocument = getTablesByName().get(tableName);
            if (tableDocument == null)
                return Optional.empty();

            return Optional.of(new SimpleSheetTable(tableDocument)) ;
        }

    }
    
    private class SimpleSheetTable implements SheetTable {
        private final String name;
        private final SpreadsheetA1Range reference;

        public SimpleSheetTable(Document tableDocument) {
            Element rootElement = tableDocument.getDocumentElement();
            this.name = getAttributeValueOrCry(rootElement, "name");
            this.reference = SpreadsheetA1Range.parse(getAttributeValueOrCry(tableDocument.getDocumentElement(), "ref"));
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
    
    private class SimpleSheetRow implements SheetRow {
        private final int rowNumber;
        private final Document sheetDoc;
        private final Element row;

        private int lastColumnIndex;

        private SimpleSheetRow(int rowNumber, Element rowElement, Document sheetDoc) {
            this.rowNumber = rowNumber;
            this.sheetDoc = sheetDoc;
            this.row = rowElement;
            this.lastColumnIndex = XmlUtils.stream(getCells()).mapToInt(this::getColumnIndex).max().orElse(-1);
        }
        
        @Override
        public void addColumn(String value) {
            addColumn(CellValues.string(value, sharedStrings));
        }

        @Override
        public void addColumn(Number value) {
            addColumn(CellValues.number(value));
        }

        @Override
        public void addColumn(Boolean value) {
            addColumn(CellValues.bool(value));
        }

        @Override
        public void addColumn(LocalDateTime value) {
            addColumn(CellValues.dateTime(value, workbookEditor.isDate1904(), stylesEditor));
        }

        @Override
        public void addColumn(LocalDate value) {
            addColumn(CellValues.date(value, workbookEditor.isDate1904(), stylesEditor)); 
        }

        @Override
        public void addFormula(String formula) {
            addColumn(CellValues.formula(formula));
        }

        @Override
        public void setValue(String columnLetter, String value) {
            setValue(columnLetter, CellValues.string(value, sharedStrings));
        }

        @Override
        public void setValue(String columnLetter, Number value) {
            setValue(columnLetter, CellValues.number(value));
        }

        @Override
        public void setValue(String columnLetter, Boolean value) {
            setValue(columnLetter, CellValues.bool(value));
        }

        @Override
        public void setValue(String columnLetter, LocalDateTime value) {
            setValue(columnLetter, CellValues.dateTime(value, workbookEditor.isDate1904(), stylesEditor));
        }

        @Override
        public void setValue(String columnLetter, LocalDate value) {
            setValue(columnLetter, CellValues.date(value, workbookEditor.isDate1904(), stylesEditor));
        }

        @Override
        public void setFormula(String columnLetter, String value) {
            setValue(columnLetter, CellValues.formula(value));
        }

        private void addColumn(CellValue cellValue) {
            Element cellElement = createCell(lastColumnIndex + 1);
            cellValue.writeValue(cellElement, sheetDoc);
        }

        private void setValue(String columnLetter, CellValue value) {
            Optional<Element> existingCellElement = getCellElementByColumnLetter(columnLetter);
            Element cellElement = existingCellElement.orElseGet(() -> createCell(columnLetterToIndex(columnLetter)));

            value.writeValue(cellElement, sheetDoc);
        }

        private Element createCell(int columnIndex) {
            Element cellElement = sheetDoc.createElement("c");
            cellElement.setAttribute("r", columnIndexToLetter(columnIndex) + rowNumber);
            
            if (columnIndex > lastColumnIndex) {
                row.appendChild(cellElement);
                row.setAttribute("spans", "1:" + (columnIndex + 1));

                lastColumnIndex = columnIndex;
            } else {
                Node nextCelllement = getNextCellElement(columnIndex);
                row.insertBefore(cellElement, nextCelllement);
            }

            return cellElement;
        }

        private NodeList getCells() {
            return row.getElementsByTagName("c");
        }
        
        private Optional<Element> getCellElementByColumnLetter(String columnLetter) {
            return XmlUtils.stream(getCells())
                    .filter(cell -> columnLetter.equals(getCellReference(cell).getColumnLetter()))
                    .map(cell -> (Element) cell)
                    .findFirst();
        }
        
        private SpreadsheetA1 getCellReference(Node cell) {
            return SpreadsheetA1.parse(getAttributeValueOrCry(cell, "r"));
        }
        
        private int getColumnIndex(Node cell) {
            return getCellReference(cell).getColumnIndex();
        }

        private Node getNextCellElement(int columnIndex) {
            return XmlUtils.stream(getCells())
                    .filter(cell -> getColumnIndex(cell) > columnIndex)
                    .sorted(comparing(this::getColumnIndex))
                    .findFirst()
                    .orElse(null);
        }
    }

    private class WorkbookEditor {

        private static final String PATH_WORKBOOK = "xl/workbook.xml";

        private boolean loaded = false;
        private boolean date1904;

        public boolean isDate1904() {
            ensureLoaded();
            return date1904;
        }

        private void ensureLoaded() {
            if(!loaded) {
                try (InputStream inputStream = new FileInputStream(new File(extractedSheetDirectory, PATH_WORKBOOK))) {
                    Document doc = XmlUtils.asDocument(inputStream);
                    Element node = (Element) doc.getElementsByTagName("workbookPr").item(0);
                    date1904 = node.hasAttribute("date1904") && asBoolean(node.getAttribute("date1904"));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                loaded = true;
            }
        }
    }

    protected class SimpleSpreadsheetStylesEditor implements SpreadsheetStylesEditor {
        private static final String PATH_STYLES = "xl/styles.xml";
        private static final int FIRST_numFmtId = 164;

        private boolean loaded = false;
        private boolean modified = false;
        private BiMap<Integer, String> numberFormat;
        private int initialNumberFormatCount;

        private List<String> styleIndexes;
        private Multimap<Integer, Integer> styleMap;
        private int initialStylesCount;

        @Override
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

        @Override
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

        @Override
        public String getPathStyles() {
            return PATH_STYLES;
        }
    }

    private boolean asBoolean(String value) {
        return "1".equals(value) || "true".equals(value);
    }

    private static File getRelsFile(File file) {
        return new File(file.getParent(), "_rels/" + file.getName() + ".rels");
    }
}