package objective.taskboard.spreadsheet;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import objective.taskboard.utils.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static objective.taskboard.utils.IOUtilities.write;

class SpreadsheetStylesEditor extends Editor {

    protected static final String PATH_STYLES = "xl/styles.xml";
    private static final int FIRST_numFmtId = 164;

    private BiMap<Integer, String> numberFormat;
    private int initialNumberFormatCount;

    private List<String> styleIndexes;
    private Multimap<Integer, Integer> styleMap;
    private int initialStylesCount;

    public SpreadsheetStylesEditor(SimpleSpreadsheetEditor spreadsheetEditor) {
        super(spreadsheetEditor);
    }

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
    protected void doLoad() {
        numberFormat = loadInternalFormats();
        styleIndexes = new ArrayList<>();
        styleMap = HashMultimap.create();

        try (InputStream inputStream = new FileInputStream(new File(spreadsheetEditor.extractedSheetDirectory, PATH_STYLES))) {
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
    }

    @Override
    protected void doSave() {
        File fileSharedStrings = new File(spreadsheetEditor.extractedSheetDirectory, PATH_STYLES);
        write(fileSharedStrings, generateStyles());
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
        try (InputStream inputStream = new FileInputStream(new File(spreadsheetEditor.extractedSheetDirectory, PATH_STYLES))) {
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