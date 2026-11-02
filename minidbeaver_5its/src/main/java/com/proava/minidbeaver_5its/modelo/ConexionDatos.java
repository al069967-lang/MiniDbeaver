package com.proava.minidbeaver_5its.modelo;

public class ConexionDatos {
    // Datos pal SSH
    private String hostSsh;
    private int puertoSsh;
    private String userSsh;
    private String passSsh;

    // Datos de la bd MariaDB
    private String hostDb;
    private int puertoDb;
    private String nameBd;
    private String userDb;
    private String passDb;

    public ConexionDatos(String hostSsh, int puertoSsh, String userSsh, String passSsh,
                         String hostDb, int puertoDb, String nameBd, String userDb, String passDb) {
        this.hostSsh = hostSsh;
        this.puertoSsh = puertoSsh;
        this.userSsh = userSsh;
        this.passSsh = passSsh;
        this.hostDb = hostDb;
        this.puertoDb = puertoDb;
        this.nameBd = nameBd;
        this.userDb = userDb;
        this.passDb = passDb;
    }

    // Puros getters autogenerados para sacar la info despues
    public String getHostSsh() { return hostSsh; }
    public int getPuertoSsh() { return puertoSsh; }
    public String getUserSsh() { return userSsh; }
    public String getPassSsh() { return passSsh; }
    public String getHostDb() { return hostDb; }
    public int getPuertoDb() { return puertoDb; }
    public String getNameBd() { return nameBd; }
    public String getUserDb() { return userDb; }
    public String getPassDb() { return passDb; }
}