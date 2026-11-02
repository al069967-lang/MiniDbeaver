package com.proava.minidbeaver_5its.controlador;

import com.proava.minidbeaver_5its.MainApp;
import com.proava.minidbeaver_5its.modelo.GestorBD;
import com.proava.minidbeaver_5its.modelo.ModeloTabla;
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

public class AppController {
    @FXML private TreeView<String> arbolTablas;
    @FXML private TableView<ObservableList<String>> tablaDatos;
    @FXML private TextArea txtSql;
    @FXML private Label statusAbajo;

    private GestorBD db;
    private String tablaElegida;
    private List<String> colActuales;

    public void meterDatos(GestorBD gestor) {
        this.db = gestor;
        refrescarMenuTablas();

        arbolTablas.getSelectionModel().selectedItemProperty().addListener((obs, v, n) -> {
            if (n != null && n.isLeaf() && n.getParent() != null) {
                tablaElegida = n.getValue();
                verTabla(tablaElegida);
            }
        });
    }

    private void refrescarMenuTablas() {
        try {
            String dbname = db.getConfigActual().getNameBd();
            TreeItem<String> root = new TreeItem<>("BD SQLite: " + dbname);
            root.setExpanded(true);

            for (String t : db.sacarTablasLocales()) {
                root.getChildren().add(new TreeItem<>(t));
            }
            arbolTablas.setRoot(root);
            statusAbajo.setText("Ya se armo, tablas locales cargadas.");
        } catch (SQLException e) {
            alertaLoca("Error we", e.getMessage());
        }
    }

    private void verTabla(String t) {
        try {
            ModeloTabla mt = db.leerDatos(t);
            rellenarGrid(mt);
            statusAbajo.setText("Viendo tabla: " + t + " (" + mt.getFilas().size() + " rows)");
        } catch (SQLException e) {
            alertaLoca("Falla al cargar", e.getMessage());
        }
    }

    private void rellenarGrid(ModeloTabla mt) {
        tablaDatos.getColumns().clear();
        tablaDatos.getItems().clear();
        colActuales = mt.getColumnas();

        for (int i = 0; i < colActuales.size(); i++) {
            final int pos = i;
            TableColumn<ObservableList<String>, String> column = new TableColumn<>(colActuales.get(i));
            column.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().get(pos)));
            column.setPrefWidth(125);
            tablaDatos.getColumns().add(column);
        }

        ObservableList<ObservableList<String>> obs = FXCollections.observableArrayList();
        for (List<String> f : mt.getFilas()) {
            obs.add(FXCollections.observableArrayList(f));
        }
        tablaDatos.setItems(obs);
    }

    @FXML
    private void onSync() {
        try {
            db.descargarBaseDatos();
            refrescarMenuTablas();
            if (tablaElegida != null) verTabla(tablaElegida);
            statusAbajo.setText("Refresh chingon.");
        } catch (SQLException e) {
            alertaLoca("Fallo la sync", e.getMessage());
        }
    }

    @FXML
    private void onCorrerQuery() {
        String q = txtSql.getText().trim();
        if (q.isEmpty()) return;

        try {
            if (q.toUpperCase().startsWith("SELECT") || q.toUpperCase().startsWith("PRAGMA")) {
                ModeloTabla mt = db.correrSelect(q);
                rellenarGrid(mt);
                statusAbajo.setText("Select corrio bien.");
            } else {
                int aff = db.correrUpdate(q);
                refrescarMenuTablas();
                if (tablaElegida != null) verTabla(tablaElegida);
                statusAbajo.setText("Se chingaron " + aff + " filas.");
            }
        } catch (SQLException e) {
            alertaLoca("Error de sintaxis SQL", e.getMessage());
        }
    }

    @FXML
    private void onAdd() {
        if (tablaElegida == null || colActuales.isEmpty()) {
            alertaLoca("Ponte pilas", "Elige una tabla del lado izquierdo primero.");
            return;
        }

        Map<String, String> data = sacarVentanaFills("Insertar en " + tablaElegida, null);
        if (data != null) {
            try {
                db.insertar(tablaElegida, data);
                verTabla(tablaElegida);
            } catch (SQLException e) {
                alertaLoca("Fallo el insert", e.getMessage());
            }
        }
    }

    @FXML
    private void onEdit() {
        ObservableList<String> fElegida = tablaDatos.getSelectionModel().getSelectedItem();
        if (tablaElegida == null || fElegida == null) {
            alertaLoca("Aviso", "Selecciona una row de la tabla we.");
            return;
        }

        Map<String, String> data = sacarVentanaFills("Modificando en " + tablaElegida, fElegida);
        if (data != null) {
            try {
                // asumo que la llave primaria es la columna 0. Ojo con eso.
                String colPk = colActuales.get(0);
                String valViejo = fElegida.get(0);
                db.actualizar(tablaElegida, data, colPk, valViejo);
                verTabla(tablaElegida);
            } catch (SQLException e) {
                alertaLoca("Fallo al hacer el update", e.getMessage());
            }
        }
    }

    @FXML
    private void onDel() {
        ObservableList<String> fkElegida = tablaDatos.getSelectionModel().getSelectedItem();
        if (tablaElegida == null || fkElegida == null) {
            alertaLoca("Cuidado", "Que quieres borrar si no elegiste nada?");
            return;
        }

        try {
            String colPk = colActuales.get(0);
            String valPk = fkElegida.get(0);
            db.borrar(tablaElegida, colPk, valPk);
            verTabla(tablaElegida);
            statusAbajo.setText("Registro eliminado.");
        } catch (SQLException e) {
            alertaLoca("No se pudo borrar", e.getMessage());
        }
    }

    @FXML
    private void onSalirBtn() {
        try {
            db.matarConexiones();
            MainApp.mostrarLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> sacarVentanaFills(String title, List<String> existingValues) {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Map<String, TextField> fields = new LinkedHashMap<>();
        for (int i = 0; i < colActuales.size(); i++) {
            String colName = colActuales.get(i);
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

    private void alertaLoca(String t, String men) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(men);
        a.showAndWait();
    }
}