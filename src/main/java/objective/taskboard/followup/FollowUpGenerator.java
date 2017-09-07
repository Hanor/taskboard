/*-
 * [LICENSE]
 * Taskboard
 * ---
 * Copyright (C) 2015 - 2017 Objective Solutions
 * ---
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * [/LICENSE]
 */
package objective.taskboard.followup;

import static java.util.stream.Collectors.toList;
import static objective.taskboard.utils.IOUtilities.write;
import static objective.taskboard.utils.ZipUtils.unzip;
import static objective.taskboard.utils.ZipUtils.zip;
import static org.apache.commons.lang.ObjectUtils.defaultIfNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import objective.taskboard.utils.IOUtilities;
import objective.taskboard.utils.XmlUtils;

public class FollowUpGenerator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FollowUpGenerator.class);

    private static final String PROPERTY_XML_SPACE_PRESERVE = " xml:space=\"preserve\"";
    private static final String TAG_T_IN_SHARED_STRINGS = "t";

    private static final String PATH_SHEET7 = "xl/worksheets/sheet7.xml";
    private static final String PATH_SHARED_STRINGS = "xl/sharedStrings.xml";
    private static final String PATH_TABLE7 = "xl/tables/table7.xml";

    private final FollowUpTemplate template;
    private final FollowupDataProvider provider;

    public FollowUpGenerator(FollowupDataProvider provider, FollowUpTemplate template) {
        this.provider = provider;
        this.template = template;
    }

    public Resource generate(String [] includedProjects) throws Exception {
        File directoryTempFollowup = null;
        Path pathFollowupXLSM = null;
        try {
            directoryTempFollowup = decompressTemplate().toFile();

            Map<String, Long> sharedStrings = getSharedStringsInitial();

            File fileSheet7 = new File(directoryTempFollowup, PATH_SHEET7);
            List<FollowUpData> jiraData = provider.getJiraData(includedProjects);
            write(fileSheet7, generateJiraDataSheet(sharedStrings, jiraData));
            File fileSharedStrings = new File(directoryTempFollowup, PATH_SHARED_STRINGS);
            write(fileSharedStrings, generateSharedStrings(sharedStrings));
            File table7 = new File(directoryTempFollowup, PATH_TABLE7);
            write(table7, generateTable7(FileUtils.readFileToString(table7, "UTF-8"), jiraData.size()));

            pathFollowupXLSM = compress(directoryTempFollowup.toPath());
            return IOUtilities.asResource(Files.readAllBytes(pathFollowupXLSM));
        } catch (Exception e) {
            log.error(e.getMessage() == null ? e.toString() : e.getMessage());
            throw e;
        } finally {
            if (directoryTempFollowup != null && directoryTempFollowup.exists())
                FileUtils.deleteDirectory(directoryTempFollowup);
            if (pathFollowupXLSM != null && pathFollowupXLSM.toFile().exists())
                Files.delete(pathFollowupXLSM);
        }
    }

    private Path decompressTemplate() throws Exception {
        Path pathFollowup = Files.createTempDirectory("Followup");
        unzip(template.getPathFollowupTemplateXLSM().getInputStream(), pathFollowup);
        return pathFollowup;
    }

    private Path compress(Path directoryFollowup) throws Exception {
        Path pathFollowupXLSM = Files.createTempFile("Followup", ".xlsm");
        zip(directoryFollowup, pathFollowupXLSM);
        return pathFollowupXLSM;
    }

    Map<String, Long> getSharedStringsInitial() throws ParserConfigurationException, SAXException, IOException {
        Map<String, Long> sharedStrings = new HashMap<>();
        Document doc = XmlUtils.asDocument(template.getPathSharedStringsInitial().getInputStream());
        doc.getDocumentElement().normalize();
        NodeList nodes = doc.getElementsByTagName(TAG_T_IN_SHARED_STRINGS);

        for (Long index = 0L; index < nodes.getLength(); index++) {
            Node node = nodes.item(index.intValue());
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            sharedStrings.put(node.getTextContent(), index);
        }
        return sharedStrings;
    }

    String generateJiraDataSheet(Map<String, Long> sharedStrings, String [] includedProjects) {
        List<FollowUpData> jiraData = provider.getJiraData(includedProjects);
        return generateJiraDataSheet(sharedStrings, jiraData);
    }

    String generateJiraDataSheet(Map<String, Long> sharedStrings, List<FollowUpData> jiraData) {
        String rowTemplate = getStringFromXML(template.getPathSheet7RowTemplate());
        StringBuilder rows = new StringBuilder();
        int rowNumber = 2;

        for (FollowUpData followUpData : jiraData) {
            Map<String, Object> rowValues = new HashMap<>();
            rowValues.put("rowNumber", rowNumber);
            rowValues.put("project", getOrSetIndexInSharedStrings(sharedStrings, followUpData.project));
            rowValues.put("demandType", getOrSetIndexInSharedStrings(sharedStrings, followUpData.demandType));
            rowValues.put("demandStatus", getOrSetIndexInSharedStrings(sharedStrings, followUpData.demandStatus));
            rowValues.put("demandNum", getOrSetIndexInSharedStrings(sharedStrings, followUpData.demandNum));
            rowValues.put("demandSummary", getOrSetIndexInSharedStrings(sharedStrings, followUpData.demandSummary));
            rowValues.put("demandDescription", getOrSetIndexInSharedStrings(sharedStrings, followUpData.demandDescription));
            rowValues.put("taskType", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskType));
            rowValues.put("taskStatus", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskStatus));
            rowValues.put("taskNum", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskNum));
            rowValues.put("taskSummary", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskSummary));
            rowValues.put("taskDescription", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskDescription));
            rowValues.put("taskFullDescription", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskFullDescription));
            rowValues.put("subtaskType", getOrSetIndexInSharedStrings(sharedStrings, followUpData.subtaskType));
            rowValues.put("subtaskStatus", getOrSetIndexInSharedStrings(sharedStrings, followUpData.subtaskStatus));
            rowValues.put("subtaskNum", getOrSetIndexInSharedStrings(sharedStrings, followUpData.subtaskNum));
            rowValues.put("subtaskSummary", getOrSetIndexInSharedStrings(sharedStrings, followUpData.subtaskSummary));
            rowValues.put("subtaskDescription", getOrSetIndexInSharedStrings(sharedStrings, followUpData.subtaskDescription));
            rowValues.put("subtaskFullDescription", getOrSetIndexInSharedStrings(sharedStrings, followUpData.subtaskFullDescription));
            rowValues.put("demandId", defaultIfNull(followUpData.demandId, ""));
            rowValues.put("taskId", defaultIfNull(followUpData.taskId, ""));
            rowValues.put("subtaskId", defaultIfNull(followUpData.subtaskId, ""));
            rowValues.put("planningType", getOrSetIndexInSharedStrings(sharedStrings, followUpData.planningType));
            rowValues.put("taskRelease", getOrSetIndexInSharedStrings(sharedStrings, followUpData.taskRelease));
            rowValues.put("worklog", defaultIfNull(followUpData.worklog, ""));
            rowValues.put("wrongWorklog", defaultIfNull(followUpData.wrongWorklog, ""));
            rowValues.put("demandBallpark", defaultIfNull(followUpData.demandBallpark, ""));
            rowValues.put("taskBallpark", defaultIfNull(followUpData.taskBallpark, ""));
            rowValues.put("tshirtSize", getOrSetIndexInSharedStrings(sharedStrings, followUpData.tshirtSize));
            rowValues.put("queryType", getOrSetIndexInSharedStrings(sharedStrings, followUpData.queryType));
            rows.append(StrSubstitutor.replace(rowTemplate, rowValues));
            rowNumber++;
        }

        String sheetTemplate = getStringFromXML(template.getPathSheet7Template());
        Map<String, String> sheetValues = new HashMap<>();
        sheetValues.put("rows", rows.toString());
        return StrSubstitutor.replace(sheetTemplate, sheetValues);
    }

    private String getStringFromXML(Resource pathXML) {
        try {
            return IOUtils.toString(pathXML.getInputStream(), "UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object getOrSetIndexInSharedStrings(Map<String, Long> sharedStrings, String followUpDataAttrValue) {
        if (followUpDataAttrValue == null || followUpDataAttrValue.isEmpty())
            return "";

        Long index = sharedStrings.get(followUpDataAttrValue);
        if (index != null)
            return index;

        index = Long.valueOf(sharedStrings.size());
        sharedStrings.put(followUpDataAttrValue, index);
        return index;
    }

    String generateSharedStrings(Map<String, Long> sharedStrings) throws IOException {
        String siSharedStringsTemplate = getStringFromXML(template.getPathSISharedStringsTemplate());
        List<String> sharedStringsSorted = sharedStrings.keySet().stream()
            .sorted((s1, s2) -> sharedStrings.get(s1).compareTo(sharedStrings.get(s2)))
            .collect(toList());
        StringBuilder allSharedStrings = new StringBuilder();

        for (String sharedString : sharedStringsSorted) {
            Map<String, Object> siValues = new HashMap<>();
            siValues.put("preserve", sharedString.endsWith(" ") ? PROPERTY_XML_SPACE_PRESERVE : "");
            siValues.put("sharedString", StringEscapeUtils.escapeXml(sharedString));
            allSharedStrings.append(StrSubstitutor.replace(siSharedStringsTemplate, siValues));
        }

        String sharedStringsTemplate = getStringFromXML(template.getPathSharedStringsTemplate());
        Map<String, Object> sharedStringsValues = new HashMap<>();
        sharedStringsValues.put("sharedStringsSize", sharedStringsSorted.size());
        sharedStringsValues.put("allSharedStrings", allSharedStrings.toString());
        return StrSubstitutor.replace(sharedStringsTemplate, sharedStringsValues);
    }

    // for now, keep the original table7 to avoid corruption
    String generateTable7(String originalTable7, int lineCount) {
        return originalTable7;    
    }

    @SuppressWarnings("unused")
    private int computeLineCount(String originalTable7, int lineCount) {
        String ref = parseLineCountFromXmlString(originalTable7);
        int oldLineCount = Integer.parseInt(ref);
        if (oldLineCount > lineCount)
            lineCount = oldLineCount-1;
        return lineCount;
    }

    private String parseLineCountFromXmlString(String originalTable7) {
        try {
            NodeList nodeList = XmlUtils.xpath(originalTable7, "/table/autoFilter/@ref");
            return XmlUtils.asString(nodeList).replace("A1:AS", "");
        } catch (TransformerException e) {
            throw new IllegalStateException(e);
        }
    }
}
