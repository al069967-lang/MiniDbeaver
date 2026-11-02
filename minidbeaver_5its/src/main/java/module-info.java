module com.proava.minidbeaver_5its {
    requires com.jcraft.jsch;
    requires java.sql;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.mariadb.jdbc;
    requires org.xerial.sqlitejdbc;

    opens com.proava.minidbeaver_5its to javafx.fxml;
    opens com.proava.minidbeaver_5its.controlador to javafx.fxml;
    opens com.proava.minidbeaver_5its.modelo to javafx.base;

    exports com.proava.minidbeaver_5its;
    exports com.proava.minidbeaver_5its.controlador;
    exports com.proava.minidbeaver_5its.modelo;
}