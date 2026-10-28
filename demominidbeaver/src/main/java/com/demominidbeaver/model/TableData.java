package com.demominidbeaver.model;

import java.util.List;

public class TableData {
    private final List<String> columnNames;
    private final List<List<String>> rows;

    public TableData(List<String> columnNames, List<List<String>> rows) {
        this.columnNames = columnNames;
        this.rows = rows;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<List<String>> getRows() {
        return rows;
    }
}
