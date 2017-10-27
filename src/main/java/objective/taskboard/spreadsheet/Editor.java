package objective.taskboard.spreadsheet;

import java.io.IOException;

public abstract class Editor {

    protected boolean loaded = false;
    protected boolean modified = false;
    protected final boolean alwaysSave;
    protected final SimpleSpreadsheetEditor spreadsheetEditor;

    protected Editor(SimpleSpreadsheetEditor spreadsheetEditor) {
        this(spreadsheetEditor, false);
    }

    protected Editor(SimpleSpreadsheetEditor spreadsheetEditor, boolean alwaysSave) {
        this.spreadsheetEditor = spreadsheetEditor;
        this.alwaysSave = alwaysSave;
    }

    protected void ensureLoaded() {
        if (!loaded) {
            doLoad();
            loaded = true;
        }
    }

    public void save() throws IOException {
        if (alwaysSave || modified) {
            doSave();
            modified = false;
            loaded = false;
        }
    }

    protected abstract void doLoad();
    protected abstract void doSave() throws IOException;
}
