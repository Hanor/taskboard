/*-
 * [LICENSE]
 * Taskboard
 * - - -
 * Copyright (C) 2015 - 2016 Objective Solutions
 * - - -
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

import java.nio.file.Path;
import java.util.Locale;

import javax.xml.transform.TransformerException;

import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.w3c.dom.NodeList;

import objective.taskboard.utils.XmlUtils;


public class FollowUpTemplateValidator {

    public void validate(Path path) {
        if(!path.toFile().exists())
            throw new InvalidTemplateException();
        // The following files must exist
        resolve(path, "xl/sharedStrings.xml");
        Path wbXml = resolve(path, "xl/workbook.xml");
        Path wbRelXml = resolve(path, "xl/_rels/workbook.xml.rels");

        // Must exist a worksheet with name "From Jira"
        String relId = xpath(wbXml, "//sheet[@name='From Jira']/@id", MessageKey.WORKSHEET_NOT_FOUND, "From Jira");
        String sheetId = xpath(wbRelXml, "//Relationship[@Id='" + relId + "']/@Target", MessageKey.CORRUPTED_FILE);
        Path sheetXml = resolve(path, "xl/" + sheetId);
        // "From Jira" must be empty
        xpathEmpty(sheetXml, "//sheetData/row[@r>1]/c/v/text()");
    }

    // ---

    private static boolean isEmpty(NodeList nodeList) {
        return nodeList.getLength() == 0;
    }

    private static Path resolve(Path base, String other) {
        Path path = base.resolve(other);
        if(!path.toFile().exists())
            throw new InvalidTemplateException(MessageKey.PATH_NOT_FOUND, other);
        return path;
    }

    private static String xpath(Path xmlFile, String locator, MessageKey key, Object... args) {
        NodeList content = XmlUtils.xpath(xmlFile.toFile(), locator);
        if(isEmpty(content))
            throw new InvalidTemplateException(key, args);
        try {
            return XmlUtils.asString(content);
        } catch (TransformerException e) {
            throw new InvalidTemplateException(e, key, args);
        }
    }

    private static void xpathEmpty(Path sheetXml, String locator) {
        NodeList content = XmlUtils.xpath(sheetXml.toFile(), locator);
        if(!isEmpty(content)) {
            throw new InvalidTemplateException(MessageKey.WORKSHEET_NOT_EMPTY, "From Jira");
        }
    }

    public enum MessageKey {
        CORRUPTED_FILE("invalid.template.corrupted")
        , PATH_NOT_FOUND("invalid.template.path-not-found")
        , WORKSHEET_NOT_FOUND("invalid.template.worksheet-not-found")
        , WORKSHEET_NOT_EMPTY("invalid.template.worksheet-not-empty");

        private final String code;

        MessageKey(String key) {
            this.code = key;
        }
    }

    public static class InvalidTemplateException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public static final String DEFAULT_MESSAGE = "Invalid file, cannot be used as template";
        private static final MessageSource MESSAGES;

        static {
            StaticMessageSource source = new StaticMessageSource();
            source.addMessage(MessageKey.CORRUPTED_FILE.code, Locale.ENGLISH, "Invalid file, seems to be corrupted");
            source.addMessage(MessageKey.PATH_NOT_FOUND.code, Locale.ENGLISH, "Invalid file, could not find path \"{0}\" within template");
            source.addMessage(MessageKey.WORKSHEET_NOT_FOUND.code, Locale.ENGLISH, "Invalid template, Worksheet \"{0}\" could not be found");
            source.addMessage(MessageKey.WORKSHEET_NOT_EMPTY.code, Locale.ENGLISH, "Invalid template, Worksheet \"{0}\" should be empty");
            MESSAGES = source;
        }

        public InvalidTemplateException() {
            super(DEFAULT_MESSAGE);
        }
        public InvalidTemplateException(Exception e) {
            super(DEFAULT_MESSAGE, e);
        }
        public InvalidTemplateException(MessageKey key, Object... args) {
            super(MESSAGES.getMessage(key.code, args, DEFAULT_MESSAGE, Locale.ENGLISH));
        }
        public InvalidTemplateException(Throwable cause, MessageKey key, Object... args) {
            super(MESSAGES.getMessage(key.code, args, DEFAULT_MESSAGE, Locale.ENGLISH), cause);
        }
    }
}
