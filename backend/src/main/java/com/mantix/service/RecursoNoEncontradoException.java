package com.mantix.service;

public class RecursoNoEncontradoException extends ErrorDeDominio {

	public RecursoNoEncontradoException(String mensaje) {
		super("NO_ENCONTRADO", mensaje);
	}

}
