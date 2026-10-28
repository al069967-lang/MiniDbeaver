package com.demominidbeaver.controller;

import com.demominidbeaver.MainApp;
import com.demominidbeaver.model.DatabaseManager;
import com.demominidbeaver.model.SshConnectionConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField sshHostField;
    @FXML private TextField sshPortField;
    @FXML private TextField sshUserField;
    @FXML private PasswordField sshPassField;

    @FXML private TextField dbHostField;
    @FXML private TextField dbPortField;
    @FXML private TextField dbNameField;
    @FXML private TextField dbUserField;
    @FXML private PasswordField dbPassField;

    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        sshPortField.setText("22");
        dbHostField.setText("127.0.0.1");
        dbPortField.setText("3306");
    }

    @FXML
    private void onConnectClick() {
        statusLabel.setText("Estableciendo túnel SSH y sincronizando base de datos...");
        try {
            int sshPort = Integer.parseInt(sshPortField.getText().trim());
            int dbPort = Integer.parseInt(dbPortField.getText().trim());

            SshConnectionConfig config = new SshConnectionConfig(
                    sshHostField.getText().trim(),
                    sshPort,
                    sshUserField.getText().trim(),
                    sshPassField.getText(),
                    dbHostField.getText().trim(),
                    dbPort,
                    dbNameField.getText().trim(),
                    dbUserField.getText().trim(),
                    dbPassField.getText()
            );

            DatabaseManager manager = new DatabaseManager();
            manager.connect(config);
            manager.syncMariaDbToSqlite();

            MainApp.showMainView(manager);
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
