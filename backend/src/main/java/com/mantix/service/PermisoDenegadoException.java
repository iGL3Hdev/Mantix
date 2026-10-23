package com.mantix.service;

public class PermisoDenegadoException extends ErrorDeDominio{

    public PermisoDenegadoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
    
}
