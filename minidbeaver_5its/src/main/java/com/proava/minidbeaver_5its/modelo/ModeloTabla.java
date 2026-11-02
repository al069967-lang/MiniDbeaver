package com.proava.minidbeaver_5its.modelo;

import java.util.List;

// Sirve pa guardar los nombres de las columnas y los datos cuando hacemos un select
public class ModeloTabla {
    private List<String> columnas;
    private List<List<String>> filas;

    public ModeloTabla(List<String> columnas, List<List<String>> filas) {
        this.columnas = columnas;
        this.filas = filas;
    }

    public List<String> getColumnas() {
        return columnas;
    }

    public List<List<String>> getFilas() {
        return filas;
    }
}
