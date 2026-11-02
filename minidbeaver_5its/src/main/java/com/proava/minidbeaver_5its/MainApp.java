package com.proava.minidbeaver_5its;

import javafx.application.Application;
import com.proava.minidbeaver_5its.controlador.AppController;
import com.proava.minidbeaver_5its.modelo.GestorBD;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    private static Stage ventanaPrincipal;

    @Override
    public void start(Stage stage) throws IOException {
        ventanaPrincipal = stage;
        mostrarLogin();
    }

    public static void mostrarLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("login-view.fxml"));
        Scene scene = new Scene(loader.load(), 500, 500);
        ventanaPrincipal.setTitle("Conexion Proyecto - Base de Datos");
        ventanaPrincipal.setScene(scene);
        ventanaPrincipal.show();
    }

    public static void mostrarPrincipal(GestorBD manager) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("main-view.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);

        AppController controller = loader.getController();
        controller.meterDatos(manager);

        ventanaPrincipal.setTitle("Gestor SQL Local ITS");
        ventanaPrincipal.setScene(scene);
        ventanaPrincipal.centerOnScreen();
    }

    public static void main(String[] args) {
        launch();
    }
}
