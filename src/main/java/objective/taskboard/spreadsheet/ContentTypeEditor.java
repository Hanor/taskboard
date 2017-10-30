package objective.taskboard.spreadsheet;

import objective.taskboard.utils.IOUtilities;
import objective.taskboard.utils.XmlUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ContentTypeEditor extends Editor {

    private List<Pair<String, String>> newPairs;

    public ContentTypeEditor(SimpleSpreadsheetEditor spreadsheetEditor) {
        super(spreadsheetEditor);
    }

    public void addContentTypeOverride(String contentType, String partName) {
        ensureLoaded();
        newPairs.add(Pair.of(contentType, partName));
        modified = true;
    }

    @Override
    protected void doLoad() {
        newPairs = new ArrayList<>();
    }

    @Override
    protected void doSave() throws IOException {
        File contentTypeOverrideFile = new File(spreadsheetEditor.extractedSheetDirectory, "[Content_Types].xml");
        Document contentTypeOverride = XmlUtils.asDocument(contentTypeOverrideFile);
        Node root = contentTypeOverride.getElementsByTagName("Types").item(0);
        for (Pair<String, String> pair : newPairs) {
            String contentType = pair.getLeft();
            String partName = pair.getRight();

            Element override = contentTypeOverride.createElement("Override");
            override.setAttribute("ContentType", contentType);
            override.setAttribute("PartName", partName);
            root.appendChild(override);
        }
        IOUtilities.write(contentTypeOverrideFile, XmlUtils.asString(contentTypeOverride));
    }
}
