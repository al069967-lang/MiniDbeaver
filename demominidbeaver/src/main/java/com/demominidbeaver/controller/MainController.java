package com.demominidbeaver.controller;

import com.demominidbeaver.MainApp;
import com.demominidbeaver.model.DatabaseManager;
import com.demominidbeaver.model.TableData;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MainController {
    @FXML private TreeView<String> tableTreeView;
    @FXML private TableView<ObservableList<String>> dataTableView;
    @FXML private TextArea sqlTextArea;
    @FXML private Label statusLabel;

    private DatabaseManager dbManager;
    private String selectedTable;
    private List<String> currentColumns;

    public void initData(DatabaseManager manager) {
        this.dbManager = manager;
        loadTablesIntoTree();

        tableTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isLeaf() && newVal.getParent() != null) {
                selectedTable = newVal.getValue();
                loadTableContent(selectedTable);
            }
        });
    }

    private void loadTablesIntoTree() {
        try {
            String dbName = dbManager.getCurrentConfig().getDbName();
            TreeItem<String> root = new TreeItem<>("SQLite Local (" + dbName + ")");
            root.setExpanded(true);

            for (String table : dbManager.getLocalTables()) {
                root.getChildren().add(new TreeItem<>(table));
            }
            tableTreeView.setRoot(root);
            statusLabel.setText("Tablas cargadas desde SQLite local.");
        } catch (SQLException e) {
            showAlert("Error cargando tablas", e.getMessage());
        }
    }

    private void loadTableContent(String tableName) {
        try {
            TableData data = dbManager.getTableData(tableName);
            populateTableView(data);
            statusLabel.setText("Mostrando tabla: " + tableName + " (" + data.getRows().size() + " registros)");
        } catch (SQLException e) {
            showAlert("Error al leer tabla", e.getMessage());
        }
    }

    private void populateTableView(TableData data) {
        dataTableView.getColumns().clear();
        dataTableView.getItems().clear();
        currentColumns = data.getColumnNames();

        for (int i = 0; i < currentColumns.size(); i++) {
            final int colIndex = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(currentColumns.get(i));
            col.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(colIndex)));
            col.setPrefWidth(130);
            dataTableView.getColumns().add(col);
        }

        ObservableList<ObservableList<String>> items = FXCollections.observableArrayList();
        for (List<String> row : data.getRows()) {
            items.add(FXCollections.observableArrayList(row));
        }
        dataTableView.setItems(items);
    }

    @FXML
    private void onSyncClick() {
        try {
            dbManager.syncMariaDbToSqlite();
            loadTablesIntoTree();
            if (selectedTable != null) {
                loadTableContent(selectedTable);
            }
            statusLabel.setText("Sincronización desde MariaDB completada con éxito.");
        } catch (SQLException e) {
            showAlert("Error de Sincronización", e.getMessage());
        }
    }

    @FXML
    private void onExecuteSqlClick() {
        String sql = sqlTextArea.getText().trim();
        if (sql.isEmpty()) return;

        try {
            if (sql.toUpperCase().startsWith("SELECT") || sql.toUpperCase().startsWith("PRAGMA")) {
                TableData data = dbManager.executeQuery(sql);
                populateTableView(data);
                statusLabel.setText("Consulta ejecutada. Filas devueltas: " + data.getRows().size());
            } else {
                int affected = dbManager.executeUpdate(sql);
                loadTablesIntoTree();
                if (selectedTable != null) loadTableContent(selectedTable);
                statusLabel.setText("Sentencia ejecutada. Filas afectadas: " + affected);
            }
        } catch (SQLException e) {
            showAlert("Error en SQL", e.getMessage());
        }
    }

    @FXML
    private void onInsertClick() {
        if (selectedTable == null || currentColumns == null || currentColumns.isEmpty()) {
            showAlert("Aviso", "Selecciona primero una tabla en el panel izquierdo.");
            return;
        }

        Map<String, String> values = showRecordDialog("Insertar en " + selectedTable, null);
        if (values != null) {
            try {
                dbManager.insertRecord(selectedTable, values);
                loadTableContent(selectedTable);
                statusLabel.setText("Registro insertado en " + selectedTable);
            } catch (SQLException e) {
                showAlert("Error al insertar", e.getMessage());
            }
        }
    }

    @FXML
    private void onUpdateClick() {
        ObservableList<String> selectedRow = dataTableView.getSelectionModel().getSelectedItem();
        if (selectedTable == null || selectedRow == null) {
            showAlert("Aviso", "Selecciona una fila de la tabla para editar.");
            return;
        }

        Map<String, String> updatedValues = showRecordDialog("Editar en " + selectedTable, selectedRow);
        if (updatedValues != null) {
            try {
                String pkCol = currentColumns.get(0);
                String pkOldVal = selectedRow.get(0);
                dbManager.updateRecord(selectedTable, updatedValues, pkCol, pkOldVal);
                loadTableContent(selectedTable);
                statusLabel.setText("Registro actualizado en " + selectedTable);
            } catch (SQLException e) {
                showAlert("Error al actualizar", e.getMessage());
            }
        }
    }

    @FXML
    private void onDeleteClick() {
        ObservableList<String> selectedRow = dataTableView.getSelectionModel().getSelectedItem();
        if (selectedTable == null || selectedRow == null) {
            showAlert("Aviso", "Selecciona una fila de la tabla para eliminar.");
            return;
        }

        try {
            String pkCol = currentColumns.get(0);
            String pkVal = selectedRow.get(0);
            dbManager.deleteRecord(selectedTable, pkCol, pkVal);
            loadTableContent(selectedTable);
            statusLabel.setText("Registro eliminado de " + selectedTable);
        } catch (SQLException e) {
            showAlert("Error al eliminar", e.getMessage());
        }
    }

    @FXML
    private void onDisconnectClick() {
        try {
            dbManager.disconnect();
            MainApp.showLoginView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> showRecordDialog(String title, List<String> existingValues) {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Map<String, TextField> fields = new LinkedHashMap<>();
        for (int i = 0; i < currentColumns.size(); i++) {
            String colName = currentColumns.get(i);
            TextField tf = new TextField(existingValues != null ? existingValues.get(i) : "");
            grid.add(new Label(colName + ":"), 0, i);
            grid.add(tf, 1, i);
            fields.put(colName, tf);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Map<String, String> result = new LinkedHashMap<>();
                fields.forEach((k, v) -> result.put(k, v.getText()));
                return result;
            }
            return null;
        });

        Optional<Map<String, String>> res = dialog.showAndWait();
        return res.orElse(null);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
