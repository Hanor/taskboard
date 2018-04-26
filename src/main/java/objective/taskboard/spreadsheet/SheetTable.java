package objective.taskboard.spreadsheet;

import objective.taskboard.google.SpreadsheetUtils.SpreadsheetA1Range;

public interface SheetTable {
    
    String getName();
    SpreadsheetA1Range getReference();

}
