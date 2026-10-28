package com.demominidbeaver.model;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DatabaseManager {
    private Session sshSession;
    private Connection mariaDbConnection;
    private Connection sqliteConnection;
    private int assignedLocalPort;
    private SshConnectionConfig currentConfig;

    private static final String SQLITE_URL = "jdbc:sqlite:demominidbeaver_local.db";

    public void connect(SshConnectionConfig config) throws Exception {
        this.currentConfig = config;

        // 1. Abrir Túnel SSH si se especificó un host SSH
        if (config.getSshHost() != null && !config.getSshHost().trim().isEmpty()) {
            JSch jsch = new JSch();
            sshSession = jsch.getSession(config.getSshUser(), config.getSshHost(), config.getSshPort());
            sshSession.setPassword(config.getSshPassword());

            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            sshSession.setConfig(prop);
            sshSession.connect(10000);

            // Asigna un puerto local libre automáticamente (0) hacia el host/puerto de MariaDB
            assignedLocalPort = sshSession.setPortForwardingL(0, config.getDbHost(), config.getDbPort());
        } else {
            assignedLocalPort = config.getDbPort();
        }

        // 2. Conectar a MariaDB (a través del puerto reenviado o directo)
        String targetHost = (sshSession != null && sshSession.isConnected()) ? "127.0.0.1" : config.getDbHost();
        String mariaUrl = String.format("jdbc:mariadb://%s:%d/%s", targetHost, assignedLocalPort, config.getDbName());
        mariaDbConnection = DriverManager.getConnection(mariaUrl, config.getDbUser(), config.getDbPassword());

        // 3. Conectar a SQLite local
        sqliteConnection = DriverManager.getConnection(SQLITE_URL);
    }

    public void syncMariaDbToSqlite() throws SQLException {
        if (mariaDbConnection == null || sqliteConnection == null) {
            throw new SQLException("Las conexiones no están inicializadas.");
        }

        List<String> tables = getRemoteTables();
        sqliteConnection.setAutoCommit(false);

        try (Statement sqliteStmt = sqliteConnection.createStatement()) {
            for (String table : tables) {
                sqliteStmt.executeUpdate("DROP TABLE IF EXISTS \"" + table + "\"");

                try (Statement mariaStmt = mariaDbConnection.createStatement();
                     ResultSet rs = mariaStmt.executeQuery("SELECT * FROM `" + table + "`")) {

                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    // Crear tabla en SQLite
                    StringBuilder createSql = new StringBuilder("CREATE TABLE \"" + table + "\" (");
                    for (int i = 1; i <= colCount; i++) {
                        createSql.append("\"").append(meta.getColumnLabel(i)).append("\" TEXT");
                        if (i < colCount) createSql.append(", ");
                    }
                    createSql.append(")");
                    sqliteStmt.executeUpdate(createSql.toString());

                    // Preparar inserción de filas
                    StringBuilder insertSql = new StringBuilder("INSERT INTO \"" + table + "\" VALUES (");
                    for (int i = 1; i <= colCount; i++) {
                        insertSql.append("?");
                        if (i < colCount) insertSql.append(", ");
                    }
                    insertSql.append(")");

                    try (PreparedStatement pstmt = sqliteConnection.prepareStatement(insertSql.toString())) {
                        while (rs.next()) {
                            for (int i = 1; i <= colCount; i++) {
                                Object val = rs.getObject(i);
                                pstmt.setString(i, val != null ? val.toString() : null);
                            }
                            pstmt.addBatch();
                        }
                        pstmt.executeBatch();
                    }
                }
            }
            sqliteConnection.commit();
        } catch (SQLException e) {
            sqliteConnection.rollback();
            throw e;
        } finally {
            sqliteConnection.setAutoCommit(true);
        }
    }

    public List<String> getRemoteTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData meta = mariaDbConnection.getMetaData();
        try (ResultSet rs = meta.getTables(currentConfig.getDbName(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    public List<String> getLocalTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'";
        try (Statement stmt = sqliteConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tables.add(rs.getString("name"));
            }
        }
        return tables;
    }

    public TableData getTableData(String tableName) throws SQLException {
        return executeQuery("SELECT * FROM \"" + tableName + "\"");
    }

    public TableData executeQuery(String sql) throws SQLException {
        List<String> columns = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        try (Statement stmt = sqliteConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    String val = rs.getString(i);
                    row.add(val != null ? val : "");
                }
                rows.add(row);
            }
        }
        return new TableData(columns, rows);
    }

    public int executeUpdate(String sql) throws SQLException {
        try (Statement stmt = sqliteConnection.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    public void insertRecord(String tableName, Map<String, String> values) throws SQLException {
        StringBuilder cols = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<String> params = new ArrayList<>();

        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!cols.isEmpty()) {
                cols.append(", ");
                placeholders.append(", ");
            }
            cols.append("\"").append(entry.getKey()).append("\"");
            placeholders.append("?");
            params.add(entry.getValue());
        }

        String sql = "INSERT INTO \"" + tableName + "\" (" + cols + ") VALUES (" + placeholders + ")";
        try (PreparedStatement pstmt = sqliteConnection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setString(i + 1, params.get(i));
            }
            pstmt.executeUpdate();
        }
    }

    public void updateRecord(String tableName, Map<String, String> newValues, String pkCol, String pkOldVal) throws SQLException {
        StringBuilder sets = new StringBuilder();
        List<String> params = new ArrayList<>();

        for (Map.Entry<String, String> entry : newValues.entrySet()) {
            if (!sets.isEmpty()) sets.append(", ");
            sets.append("\"").append(entry.getKey()).append("\" = ?");
            params.add(entry.getValue());
        }
        params.add(pkOldVal);

        String sql = "UPDATE \"" + tableName + "\" SET " + sets + " WHERE \"" + pkCol + "\" = ?";
        try (PreparedStatement pstmt = sqliteConnection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setString(i + 1, params.get(i));
            }
            pstmt.executeUpdate();
        }
    }

    public void deleteRecord(String tableName, String pkCol, String pkVal) throws SQLException {
        String sql = "DELETE FROM \"" + tableName + "\" WHERE \"" + pkCol + "\" = ?";
        try (PreparedStatement pstmt = sqliteConnection.prepareStatement(sql)) {
            pstmt.setString(1, pkVal);
            pstmt.executeUpdate();
        }
    }

    public void disconnect() {
        try { if (sqliteConnection != null && !sqliteConnection.isClosed()) sqliteConnection.close(); } catch (Exception ignored) {}
        try { if (mariaDbConnection != null && !mariaDbConnection.isClosed()) mariaDbConnection.close(); } catch (Exception ignored) {}
        try { if (sshSession != null && sshSession.isConnected()) sshSession.disconnect(); } catch (Exception ignored) {}
    }

    public SshConnectionConfig getCurrentConfig() {
        return currentConfig;
    }
}
