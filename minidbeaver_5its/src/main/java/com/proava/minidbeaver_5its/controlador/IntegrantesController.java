package com.proava.minidbeaver_5its.controlador;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.Stage;

public class IntegrantesController {

    @FXML
    private void onRegresarClick(ActionEvent event) {
        // Cierra unicamente la ventana actual
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}