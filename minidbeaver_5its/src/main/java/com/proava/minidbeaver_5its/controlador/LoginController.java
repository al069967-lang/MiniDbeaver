package com.proava.minidbeaver_5its.controlador;

import com.proava.minidbeaver_5its.MainApp;
import com.proava.minidbeaver_5its.modelo.GestorBD;
import com.proava.minidbeaver_5its.modelo.ConexionDatos;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML private TextField txtSshHost;
    @FXML private TextField txtSshPuerto;
    @FXML private TextField txtSshUser;
    @FXML private PasswordField txtSshPass;

    @FXML private TextField txtDbHost;
    @FXML private TextField txtDbPuerto;
    @FXML private TextField txtDbName;
    @FXML private TextField txtDbUser;
    @FXML private PasswordField txtDbPass;

    @FXML private Label lblAvisos;

    @FXML
    public void initialize() {
        // datos quemados pal profe
        txtSshHost.setText("fi.jcaguilar.dev");
        txtSshPuerto.setText("22");
        txtSshUser.setText("patito");
        txtSshPass.setText("FdI-its-5a$$");

        txtDbHost.setText("localhost");
        txtDbPuerto.setText("3306");
        txtDbName.setText("its5a");
        txtDbUser.setText("becario");
        txtDbPass.setText("FdI-its-5a");
    }

    @FXML
    private void darleConectar() {
        lblAvisos.setText("Aguarde un momento, conectando...");
        lblAvisos.setStyle("-fx-text-fill: green;");

        try {
            // Si lo dejan vacio, le ponemos un puerto por defecto pa que no truene el ParseInt
            String portSshStr = txtSshPuerto.getText().trim();
            int pSsh = portSshStr.isEmpty() ? 22 : Integer.parseInt(portSshStr);

            String portDbStr = txtDbPuerto.getText().trim();
            int pDb = portDbStr.isEmpty() ? 3306 : Integer.parseInt(portDbStr);

            ConexionDatos configs = new ConexionDatos(
                    txtSshHost.getText().trim(),
                    pSsh,
                    txtSshUser.getText().trim(),
                    txtSshPass.getText(),
                    txtDbHost.getText().trim(),
                    pDb,
                    txtDbName.getText().trim(),
                    txtDbUser.getText().trim(),
                    txtDbPass.getText()
            );

            GestorBD ges = new GestorBD();
            ges.conectar(configs);
            ges.descargarBaseDatos();

            // juimonos pa la otra pantalla
            MainApp.mostrarPrincipal(ges);
        } catch (NumberFormatException nfe) {
            lblAvisos.setText("Ey, pusiste letras en el puerto.");
            lblAvisos.setStyle("-fx-text-fill: red;");
        } catch (Exception e) {
            lblAvisos.setText("Error alv: " + e.getMessage());
            lblAvisos.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }

    @FXML
    private void onVerEquipoClick() {
        try {
            // Cargamos la vista de los integrantes
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(MainApp.class.getResource("lista_integrantes-view.fxml"));
            javafx.scene.Parent root = loader.load();

            // Creamos una nueva ventanita (Stage)
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Integrantes del Equipo");
            stage.setScene(new javafx.scene.Scene(root, 600, 400));

            // Bloqueamos la ventana de login mientras esta este abierta (opcional pero se ve bien)
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            System.out.println("No se pudo cargar la vista de integrantes: " + e.getMessage());
            e.printStackTrace();
        }
    }

}