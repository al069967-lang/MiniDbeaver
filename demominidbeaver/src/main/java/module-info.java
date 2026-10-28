module com.demominidbeaver {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.jcraft.jsch;
    requires org.mariadb.jdbc;
    requires org.xerial.sqlitejdbc;

    opens com.demominidbeaver to javafx.fxml;
    opens com.demominidbeaver.controller to javafx.fxml;
    opens com.demominidbeaver.model to javafx.base;

    exports com.demominidbeaver;
    exports com.demominidbeaver.controller;
    exports com.demominidbeaver.model;
}
