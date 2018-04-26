package objective.taskboard.google;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpreadsheetUtils {
	private static final String COLUMN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final Pattern A1_PATTERN = Pattern.compile("^(\\D+)(\\d+)");

	public static final Comparator<String> COLUMN_LETTER_COMPARATOR = 
	        (l1, l2) -> Integer.compare(columnLetterToIndex(l1), columnLetterToIndex(l2));

    public static int columnLetterToIndex(String column) {
        int columnNumber = 0;
        int length = column.length();
        
		if (column == null || length == 0)
            throw new IllegalArgumentException("Input is not valid!");

        for (int i = 0; i < length; i++) {
            char curr = column.charAt(i);
            columnNumber += (curr - 'A' + 1) * Math.pow(26, length - i - 1);
        }

        return columnNumber - 1;
    }

    public static String columnIndexToLetter(Integer index) {
        int base = COLUMN_CHARS.length();
        int column = index + 1;
        int temp = 0;
        StringBuilder result = new StringBuilder();
        
        while (column > 0) {
        	temp = (column - 1) % base;
        	result.insert(0, COLUMN_CHARS.charAt(temp));
        	column = (column - temp - 1) / base;
        }

        return result.toString();
    }
    
    public static class SpreadsheetA1 {
        private final String columnLetter;
        private final int rowNumber;
        private Integer columnIndex = null;

        public SpreadsheetA1(String columnLetter, int rowNumber) {
            this.columnLetter = columnLetter;
            this.rowNumber = rowNumber;
        }
        
        public static SpreadsheetA1 parse(String a1String) {
            Matcher matcher = A1_PATTERN.matcher(a1String);
            boolean found = matcher.find();
            
            if (!found || matcher.groupCount() != 2)
                throw new IllegalArgumentException("A1 notation '" + a1String + "' is not valid!");
            
            String columnLetter = matcher.group(1);
            int rowNumber = Integer.parseInt(matcher.group(2));
            
            return new SpreadsheetA1(columnLetter, rowNumber);
        }

        public String getColumnLetter() {
            return columnLetter;
        }

        public int getColumnIndex() {
            if (columnIndex == null)
                columnIndex = columnLetterToIndex(columnLetter);
            
            return columnIndex;
        }

        public int getRowNumber() {
            return rowNumber;
        }
    }
    
    public static class SpreadsheetA1Range {
        private final SpreadsheetA1 start;
        private final SpreadsheetA1 end;
        
        public SpreadsheetA1Range(SpreadsheetA1 start, SpreadsheetA1 end) {
            this.start = start;
            this.end = end;
        }
        
        public static SpreadsheetA1Range parse(String value) {
            String[] parts = value.split(":");
            
            if (parts.length == 1) {
                SpreadsheetA1 single = SpreadsheetA1.parse(parts[0]);
                return new SpreadsheetA1Range(single, single);
                
            } else if (parts.length == 2) {
                SpreadsheetA1 start = SpreadsheetA1.parse(parts[0]);
                SpreadsheetA1 end   = SpreadsheetA1.parse(parts[1]);
                return new SpreadsheetA1Range(start, end);
            }
            
            throw new IllegalArgumentException("A1 range '" + value + "' is not valid!");
        }
        
        public SpreadsheetA1 getStart() {
            return start;
        }
        
        public SpreadsheetA1 getEnd() {
            return end;
        }
    }
}
