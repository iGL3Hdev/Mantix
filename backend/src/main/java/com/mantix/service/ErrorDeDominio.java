package com.mantix.service;

public abstract class ErrorDeDominio extends RuntimeException{

    private final String codigo;

    protected ErrorDeDominio(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
    
}
