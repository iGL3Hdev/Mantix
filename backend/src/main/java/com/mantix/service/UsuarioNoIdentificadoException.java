package com.mantix.service;

public class UsuarioNoIdentificadoException extends ErrorDeDominio {

	public UsuarioNoIdentificadoException(String mensaje) {
		super("USUARIO_NO_IDENTIFICADO", mensaje);
	}

}
