package com.demominidbeaver;

import com.demominidbeaver.controller.MainController;
import com.demominidbeaver.model.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    private static Stage primaryStage;
    private static DatabaseManager dbManager;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        showLoginView();
    }

    public static void showLoginView() throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/demominidbeaver/view/login-view.fxml"));
        Scene scene = new Scene(loader.load(), 500, 560);
        primaryStage.setTitle("DemoMiniDBeaver - Conexión SSH / MariaDB");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void showMainView(DatabaseManager manager) throws Exception {
        dbManager = manager;
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/demominidbeaver/view/main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1024, 680);

        MainController controller = loader.getController();
        controller.initData(manager);

        primaryStage.setTitle("DemoMiniDBeaver - Gestor Local SQLite & Remoto MariaDB");
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        if (dbManager != null) {
            dbManager.disconnect();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
