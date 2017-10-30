package objective.taskboard.spreadsheet;

import objective.taskboard.utils.XmlUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static objective.taskboard.utils.IOUtilities.write;

class SharedStringsEditor extends Editor {

    private static final String PATH_SHARED_STRINGS = "xl/sharedStrings.xml";
    private static final String TAG_T_IN_SHARED_STRINGS = "t";

    private Map<String, Long> sharedStrings;
    private int initialSharedStringCount;

    public SharedStringsEditor(SimpleSpreadsheetEditor spreadsheetEditor) {
        super(spreadsheetEditor);
    }

    public String getOrSetIndexInSharedStrings(String followUpDataAttrValue) {
        if (StringUtils.isEmpty(followUpDataAttrValue))
            return "";

        ensureLoaded();
        Long index = sharedStrings.get(followUpDataAttrValue);
        if (index != null)
            return index+"";

        index = Long.valueOf(sharedStrings.size());
        sharedStrings.put(followUpDataAttrValue, index);
        modified = true;
        return index+"";
    }

    @Override
    protected void doLoad() {
        sharedStrings = initializeSharedStrings();
        initialSharedStringCount = sharedStrings.size();
    }

    @Override
    protected void doSave() throws IOException {
        File fileSharedStrings = new File(spreadsheetEditor.extractedSheetDirectory, PATH_SHARED_STRINGS);
        write(fileSharedStrings, generateSharedStrings());
    }

    private Map<String, Long> initializeSharedStrings() {
        File sharedStringsFile = new File(spreadsheetEditor.extractedSheetDirectory, PATH_SHARED_STRINGS);
        if(!sharedStringsFile.exists())
            return new HashMap<>();

        Map<String, Long> sharedStrings = new HashMap<>();
        try (InputStream inputStream = new FileInputStream(sharedStringsFile)) {
            Document doc = XmlUtils.asDocument(inputStream);
            doc.getDocumentElement().normalize();
            NodeList nodes = doc.getElementsByTagName(TAG_T_IN_SHARED_STRINGS);

            for (Long index = 0L; index < nodes.getLength(); index++) {
                Node node = nodes.item(index.intValue());
                if (node.getNodeType() != Node.ELEMENT_NODE)
                    continue;

                sharedStrings.put(node.getTextContent(), index);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return sharedStrings;
    }

    Map<String, Long> getSharedStrings() {
        ensureLoaded();
        return sharedStrings;
    }

    String generateSharedStrings() {
        ensureLoaded();
        File sharedStringsFile = new File(spreadsheetEditor.extractedSheetDirectory, PATH_SHARED_STRINGS);
        if(!sharedStringsFile.exists()) {
            createEmptySharedStrings();
        }
        try(InputStream inputStream = new FileInputStream(new File(spreadsheetEditor.extractedSheetDirectory, PATH_SHARED_STRINGS))) {
            Document sharedStringsDoc = XmlUtils.asDocument(inputStream);
            Element root = (Element) sharedStringsDoc.getElementsByTagName("sst").item(0);

            List<String> sharedStringsSorted = sharedStrings.keySet().stream()
                    .sorted(Comparator.comparing(s -> sharedStrings.get(s)))
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
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void createEmptySharedStrings() {
        try {
            FileUtils.write(new File(spreadsheetEditor.extractedSheetDirectory, PATH_SHARED_STRINGS),
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                            + "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"0\" uniqueCount=\"0\">\n"
                            + "</sst>",
                    "UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        createContentTypeOverride();
        spreadsheetEditor.workbookEditor.addWorkbookRel("http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings", "sharedStrings.xml");
    }

    private void createContentTypeOverride() {
        spreadsheetEditor.contentTypeEditor.addContentTypeOverride("application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml", "/"+PATH_SHARED_STRINGS);
    }
}