package objective.taskboard.google;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import objective.taskboard.google.SpreadsheetUtils.SpreadsheetA1;
import objective.taskboard.google.SpreadsheetUtils.SpreadsheetA1Range;

public class SpreadsheetUtilsTest {

    @Test
    public void columnLetterToIndex() {
        assertEquals(0, SpreadsheetUtils.columnLetterToIndex("A"));
        assertEquals(1, SpreadsheetUtils.columnLetterToIndex("B"));
        assertEquals(2, SpreadsheetUtils.columnLetterToIndex("C"));

        assertEquals(26, SpreadsheetUtils.columnLetterToIndex("AA"));
        assertEquals(27, SpreadsheetUtils.columnLetterToIndex("AB"));
        
        assertEquals(702, SpreadsheetUtils.columnLetterToIndex("AAA"));
    }

    @Test
    public void columnIndexToLetter() {
        assertEquals("A", SpreadsheetUtils.columnIndexToLetter(0));
        assertEquals("Z", SpreadsheetUtils.columnIndexToLetter(25));
        assertEquals("AA", SpreadsheetUtils.columnIndexToLetter(26));
        assertEquals("AB", SpreadsheetUtils.columnIndexToLetter(27));
    }

    @Test
    public void columnLetterComparator() {
        List<String> letters = asList("Z", "AB", "A", "AZ", "R");
        letters.sort(SpreadsheetUtils.COLUMN_LETTER_COMPARATOR);
        
        assertEquals(asList("A", "R", "Z", "AB", "AZ"), letters);
    }
    
    @Test
    public void parseA1() {
        SpreadsheetA1 a1 = SpreadsheetA1.parse("A1");

        assertEquals("A", a1.getColumnLetter());
        assertEquals(0, a1.getColumnIndex());
        assertEquals(1, a1.getRowNumber());
        
        a1 = SpreadsheetA1.parse("AZ199");

        assertEquals("AZ", a1.getColumnLetter());
        assertEquals(51, a1.getColumnIndex());
        assertEquals(199, a1.getRowNumber());
    }
    
    @Test
    public void parseA1Range() {
        SpreadsheetA1Range range = SpreadsheetA1Range.parse("A1:C9");
        
        assertEquals("A", range.getStart().getColumnLetter());
        assertEquals(1, range.getStart().getRowNumber());
        assertEquals("C", range.getEnd().getColumnLetter());
        assertEquals(9, range.getEnd().getRowNumber());
    }
    
    @Test
    public void parseA1Range_singleValue() {
        SpreadsheetA1Range range = SpreadsheetA1Range.parse("A1");
        
        assertEquals("A", range.getStart().getColumnLetter());
        assertEquals(1, range.getStart().getRowNumber());
        assertEquals("A", range.getEnd().getColumnLetter());
        assertEquals(1, range.getEnd().getRowNumber());
    }
}
