package com.proava.minidbeaver_5its.modelo;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class GestorBD {
    private Session sessionSsh;
    private Connection conexionMaria;
    private Connection conexionLocal;
    private ConexionDatos configActual;

    // aqui se va a crear el archivo de base de datos
    private String rutadb = "jdbc:sqlite:backup_proyecto.db";

    public void conectar(ConexionDatos config) throws Exception {
        this.configActual = config;
        int localPort;

        // checar si pusieron host para saber si abrimos el tunel o no
        if (config.getHostSsh() != null && !config.getHostSsh().trim().isEmpty()) {
            System.out.println("Armando el tunel SSH...");
            JSch jsch = new JSch();
            sessionSsh = jsch.getSession(config.getUserSsh(), config.getHostSsh(), config.getPuertoSsh());
            sessionSsh.setPassword(config.getPassSsh());

            // esto es pa que no pida aceptar la llave rara esa del servidor
            Properties p = new Properties();
            p.put("StrictHostKeyChecking", "no");
            sessionSsh.setConfig(p);
            sessionSsh.connect(5000);

            // el puerto 0 hace que busque uno disponible solito
            localPort = sessionSsh.setPortForwardingL(0, config.getHostDb(), config.getPuertoDb());
            System.out.println("Tunel chido. Puerto local asignado: " + localPort);
        } else {
            localPort = config.getPuertoDb();
            System.out.println("Conectando directo sin SSH...");
        }

        // nos conectamos a la BD remota
        String hostFinal = (sessionSsh != null && sessionSsh.isConnected()) ? "127.0.0.1" : config.getHostDb();
        String urlMaria = "jdbc:mariadb://" + hostFinal + ":" + localPort + "/" + config.getNameBd();

        conexionMaria = DriverManager.getConnection(urlMaria, config.getUserDb(), config.getPassDb());
        conexionLocal = DriverManager.getConnection(rutadb);
    }

    public void descargarBaseDatos() throws SQLException {
        if (conexionMaria == null || conexionLocal == null) {
            throw new SQLException("No hay conexiones activas bro");
        }

        List<String> tablas = sacarTablasRemotas();
        conexionLocal.setAutoCommit(false); // pa que no tarde mil horas insertando

        try (Statement stmLocal = conexionLocal.createStatement()) {
            for (String tbl : tablas) {
                stmLocal.executeUpdate("DROP TABLE IF EXISTS \"" + tbl + "\"");

                try (Statement stmRemoto = conexionMaria.createStatement();
                     ResultSet rs = stmRemoto.executeQuery("SELECT * FROM `" + tbl + "`")) {

                    ResultSetMetaData meta = rs.getMetaData();
                    int cantCols = meta.getColumnCount();

                    // armar el create table
                    String sqlCreate = "CREATE TABLE \"" + tbl + "\" (";
                    for (int i = 1; i <= cantCols; i++) {
                        sqlCreate += "\"" + meta.getColumnLabel(i) + "\" TEXT";
                        if (i < cantCols) sqlCreate += ", ";
                    }
                    sqlCreate += ")";
                    stmLocal.executeUpdate(sqlCreate);

                    // armar los inserts
                    String sqlInsert = "INSERT INTO \"" + tbl + "\" VALUES (";
                    for (int i = 1; i <= cantCols; i++) {
                        sqlInsert += "?";
                        if (i < cantCols) sqlInsert += ", ";
                    }
                    sqlInsert += ")";

                    // meter los datos
                    try (PreparedStatement pstLocal = conexionLocal.prepareStatement(sqlInsert)) {
                        while (rs.next()) {
                            for (int i = 1; i <= cantCols; i++) {
                                Object obj = rs.getObject(i);
                                pstLocal.setString(i, obj != null ? obj.toString() : null);
                            }
                            pstLocal.addBatch();
                        }
                        pstLocal.executeBatch();
                    }
                }
            }
            conexionLocal.commit();
            System.out.println("Descarga lista.");
        } catch (SQLException e) {
            conexionLocal.rollback(); // si algo falla regresamos todo
            throw e;
        } finally {
            conexionLocal.setAutoCommit(true);
        }
    }

    public List<String> sacarTablasRemotas() throws SQLException {
        List<String> lista = new ArrayList<>();
        DatabaseMetaData meta = conexionMaria.getMetaData();
        try (ResultSet rs = meta.getTables(configActual.getNameBd(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                lista.add(rs.getString("TABLE_NAME"));
            }
        }
        return lista;
    }

    public List<String> sacarTablasLocales() throws SQLException {
        List<String> tb = new ArrayList<>();
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'";
        try (Statement st = conexionLocal.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                tb.add(rs.getString("name"));
            }
        }
        return tb;
    }

    public ModeloTabla leerDatos(String tabla) throws SQLException {
        return correrSelect("SELECT * FROM \"" + tabla + "\"");
    }

    public ModeloTabla correrSelect(String query) throws SQLException {
        List<String> cols = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        try (Statement st = conexionLocal.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            ResultSetMetaData meta = rs.getMetaData();
            int c = meta.getColumnCount();

            for (int i = 1; i <= c; i++) {
                cols.add(meta.getColumnLabel(i));
            }

            while (rs.next()) {
                List<String> f = new ArrayList<>();
                for (int i = 1; i <= c; i++) {
                    String v = rs.getString(i);
                    f.add(v != null ? v : ""); // pa q no truene la vista
                }
                rows.add(f);
            }
        }
        return new ModeloTabla(cols, rows);
    }

    public int correrUpdate(String q) throws SQLException {
        try (Statement st = conexionLocal.createStatement()) {
            return st.executeUpdate(q);
        }
    }

    public void insertar(String t, Map<String, String> valores) throws SQLException {
        String columnas = "";
        String comodines = "";
        List<String> params = new ArrayList<>();

        for (Map.Entry<String, String> item : valores.entrySet()) {
            if (!columnas.isEmpty()) {
                columnas += ", ";
                comodines += ", ";
            }
            columnas += "\"" + item.getKey() + "\"";
            comodines += "?";
            params.add(item.getValue());
        }

        String sql = "INSERT INTO \"" + t + "\" (" + columnas + ") VALUES (" + comodines + ")";
        try (PreparedStatement pst = conexionLocal.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                pst.setString(i + 1, params.get(i));
            }
            pst.executeUpdate();
        }
    }

    public void actualizar(String t, Map<String, String> data, String pk, String idViejo) throws SQLException {
        String setStr = "";
        List<String> p = new ArrayList<>();

        for (Map.Entry<String, String> item : data.entrySet()) {
            if (!setStr.isEmpty()) setStr += ", ";
            setStr += "\"" + item.getKey() + "\" = ?";
            p.add(item.getValue());
        }
        p.add(idViejo); // para el where al final

        String query = "UPDATE \"" + t + "\" SET " + setStr + " WHERE \"" + pk + "\" = ?";
        try (PreparedStatement pst = conexionLocal.prepareStatement(query)) {
            for (int i = 0; i < p.size(); i++) {
                pst.setString(i + 1, p.get(i));
            }
            pst.executeUpdate();
        }
    }

    public void borrar(String tabla, String pkCol, String pkVal) throws SQLException {
        String query = "DELETE FROM \"" + tabla + "\" WHERE \"" + pkCol + "\" = ?";
        try (PreparedStatement pst = conexionLocal.prepareStatement(query)) {
            pst.setString(1, pkVal);
            pst.executeUpdate();
        }
    }

    public void matarConexiones() {
        System.out.println("Cerrando todo...");
        try { if (conexionLocal != null && !conexionLocal.isClosed()) conexionLocal.close(); } catch (Exception e) {}
        try { if (conexionMaria != null && !conexionMaria.isClosed()) conexionMaria.close(); } catch (Exception e) {}
        try { if (sessionSsh != null && sessionSsh.isConnected()) sessionSsh.disconnect(); } catch (Exception e) {}
    }

    public ConexionDatos getConfigActual() {
        return configActual;
    }
}
