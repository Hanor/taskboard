package objective.taskboard.spreadsheet;

import com.google.common.collect.Streams;
import objective.taskboard.utils.XmlUtils;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SpreadsheetStylesEditorTest extends AbstractEditorTest {

    @Test
    public void whenCustomFormatAdded_stylesXmlShouldContainNewFormat() throws IOException {
        try (SimpleSpreadsheetEditor editor = new SimpleSpreadsheetEditor(getBasicTemplate())) {
            editor.open();
            SpreadsheetStylesEditor subject = editor.stylesEditor;

            // given
            String dateFormat = "yyyy/mm/dd\\ hh:mm:ss";
            String currencyFormat = "[$R$-416]\\ #,##0.00;[RED]\\-[$R$-416]\\ #,##0.00";

            // when
            subject.getOrCreateNumberFormat(dateFormat);
            subject.save();
            subject.getOrCreateNumberFormat(currencyFormat);
            subject.save();

            // then
            NodeList formatList = XmlUtils.xpath(new File(editor.getExtractedSheetDirectory(), SpreadsheetStylesEditor.PATH_STYLES), "//numFmts/numFmt");
            assertThat(Streams.stream(XmlUtils.iterable(formatList))
                    .filter(node -> node instanceof Element)
                    .map(node -> (Element)node)
                    .filter(element -> dateFormat.equals(element.getAttribute("formatCode")))
                    .count(), is(1L));

            assertThat(Streams.stream(XmlUtils.iterable(formatList))
                    .filter(node -> node instanceof Element)
                    .map(node -> (Element)node)
                    .filter(element -> currencyFormat.equals(element.getAttribute("formatCode")))
                    .count(), is(1L));
        }
    }

}
