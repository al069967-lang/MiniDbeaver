package com.demominidbeaver.model;

public class SshConnectionConfig {
    private final String sshHost;
    private final int sshPort;
    private final String sshUser;
    private final String sshPassword;
    private final String dbHost;
    private final int dbPort;
    private final String dbName;
    private final String dbUser;
    private final String dbPassword;

    public SshConnectionConfig(String sshHost, int sshPort, String sshUser, String sshPassword,
                               String dbHost, int dbPort, String dbName, String dbUser, String dbPassword) {
        this.sshHost = sshHost;
        this.sshPort = sshPort;
        this.sshUser = sshUser;
        this.sshPassword = sshPassword;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public String getSshHost() { return sshHost; }
    public int getSshPort() { return sshPort; }
    public String getSshUser() { return sshUser; }
    public String getSshPassword() { return sshPassword; }
    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbName() { return dbName; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
}
