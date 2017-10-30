package objective.taskboard.spreadsheet;

import objective.taskboard.utils.IOUtilities;
import objective.taskboard.utils.XmlUtils;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static objective.taskboard.spreadsheet.SpreadsheetUtils.asBoolean;

class WorkbookEditor extends Editor {

    private static final String PATH_WORKBOOK = "xl/workbook.xml";
    private static final String PATH_WORKBOOK_REL = "xl/_rels/workbook.xml.rels";

    private Map<String,String> sheetPathByName;
    private boolean date1904;

    public WorkbookEditor(SimpleSpreadsheetEditor spreadsheetEditor) {
        super(spreadsheetEditor, true);
    }

    public boolean isDate1904() {
        ensureLoaded();
        return date1904;
    }

    public SimpleSpreadsheetEditor.Sheet getSheet(String sheetName) {
        ensureLoaded();
        return spreadsheetEditor.new Sheet(sheetPathByName.get(sheetName));
    }

    public SimpleSpreadsheetEditor.Sheet createSheet(String sheetName) {
        ensureLoaded();
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

        return spreadsheetEditor.new Sheet(sheetPath);
    }

    public SimpleSpreadsheetEditor.Sheet getOrCreateSheet(String sheetName) {
        ensureLoaded();
        if(sheetPathByName.containsKey(sheetName)) {
            return getSheet(sheetName);
        } else {
            return createSheet(sheetName);
        }
    }

    public String addWorkbookRel(String type, String target) {
        Document workbookRelsDoc = getWorkbookRelsDoc();

        Element relationship = workbookRelsDoc.createElement("Relationship");
        String relationId = "rId"+computeNextRelationId(workbookRelsDoc);
        relationship.setAttribute("Id", relationId);
        relationship.setAttribute("Target", target);
        relationship.setAttribute("Type", type);
        workbookRelsDoc.getElementsByTagName("Relationships").item(0).appendChild(relationship);
        IOUtilities.write(getWorkbookRelsFile(), XmlUtils.asString(workbookRelsDoc));

        return relationId;
    }

    @Override
    protected void doLoad() {
        try (InputStream inputStream = new FileInputStream(new File(spreadsheetEditor.extractedSheetDirectory, PATH_WORKBOOK))) {
            Document doc = XmlUtils.asDocument(inputStream);
            sheetPathByName = initializeWorkbookRelations(doc);
            date1904 = extractDateSystem(doc);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void doSave() {
        ensureFullCalcOnReloadIsSet();
    }

    private boolean extractDateSystem(Document doc) {
        Element node = (Element) doc.getElementsByTagName("workbookPr").item(0);
        return node.hasAttribute("date1904") && asBoolean(node.getAttribute("date1904"));
    }

    private String addWorkbookRel(int next) {
        return addWorkbookRel("http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet", "worksheets/sheet" + next + ".xml");
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
        spreadsheetEditor.contentTypeEditor.addContentTypeOverride("application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml", "/"+sheetPath);
    }

    private void createEmptySheet(String sheetPath) {
        try {
            FileUtils.write(new File(spreadsheetEditor.extractedSheetDirectory,sheetPath),
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

    private void ensureFullCalcOnReloadIsSet() {
        Document workbook = getWorkbook();
        Node calcPr = workbook.getElementsByTagName("calcPr").item(0);
        Attr fullCalcOnLoad = workbook.createAttribute("fullCalcOnLoad");
        fullCalcOnLoad.setValue("1");
        calcPr.getAttributes().setNamedItem(fullCalcOnLoad);
        IOUtilities.write(getWorkbookFile(), XmlUtils.asString(workbook));
    }

    private Map<String, String> initializeWorkbookRelations(final Document workbook) {
        Map<String, String> sheetPathByName = new LinkedHashMap<>();
        final Document workbookRels = getWorkbookRelsDoc();
        final NodeList relationShips = workbookRels.getElementsByTagName("Relationship");
        final Map<String,String> relations = new LinkedHashMap<>();
        for (int i = 0; i < relationShips.getLength(); i++) {
            Node sheetNode = relationShips.item(i);
            sheetNode.getAttributes().getNamedItem("Id");
            relations.put(sheetNode.getAttributes().getNamedItem("Id").getNodeValue(),
                    sheetNode.getAttributes().getNamedItem("Target").getNodeValue());
        }

        final NodeList sheetList = workbook.getElementsByTagName("sheet");
        for (int i = 0; i < sheetList.getLength(); i++) {
            Node sheetNode = sheetList.item(i);
            String id = sheetNode.getAttributes().getNamedItem("r:id").getNodeValue();
            String name = sheetNode.getAttributes().getNamedItem("name").getNodeValue();
            sheetPathByName.put(name, "xl/"+relations.get(id));
        }
        return sheetPathByName;
    }

    private Document getWorkbook() {
        return XmlUtils.asDocument(getWorkbookFile());
    }

    private File getWorkbookFile() {
        return new File(spreadsheetEditor.extractedSheetDirectory, PATH_WORKBOOK);
    }

    private Document getWorkbookRelsDoc() {
        return XmlUtils.asDocument(getWorkbookRelsFile());
    }

    private File getWorkbookRelsFile() {
        return new File(spreadsheetEditor.extractedSheetDirectory, PATH_WORKBOOK_REL);
    }

}