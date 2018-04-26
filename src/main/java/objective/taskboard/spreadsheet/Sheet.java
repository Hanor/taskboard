package objective.taskboard.spreadsheet;

import java.util.Optional;

public interface Sheet {

    void save();

    SheetRow createRow();
    
    SheetRow getOrCreateRow(int rowNumber);

    void truncate();

    String getSheetPath();
    
    Optional<SheetTable> getTable(String tableName);

}