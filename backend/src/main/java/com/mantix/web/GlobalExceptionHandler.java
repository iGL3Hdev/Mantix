package com.mantix.web;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.mantix.service.ErrorDeDominio;
import com.mantix.service.PermisoDenegadoException;
import com.mantix.service.RecursoNoEncontradoException;
import com.mantix.service.ReglaNegocioException;
import com.mantix.service.UsuarioNoIdentificadoException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ReglaNegocioException.class)
	ProblemDetail reglaIncumplida(ReglaNegocioException ex) {
		return problema(HttpStatus.CONFLICT, "Regla de negocio incumplida", ex);
	}

	@ExceptionHandler(PermisoDenegadoException.class)
	ProblemDetail permisoDenegado(PermisoDenegadoException ex) {
		return problema(HttpStatus.FORBIDDEN, "Acción no permitida", ex);
	}

	@ExceptionHandler(RecursoNoEncontradoException.class)
	ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex);
	}

	@ExceptionHandler(UsuarioNoIdentificadoException.class)
	ProblemDetail sinUsuario(UsuarioNoIdentificadoException ex) {
		return problema(HttpStatus.UNAUTHORIZED, "Usuario no identificado", ex);
	}

	// Último recurso: cualquier error que nadie haya previsto. El detalle va al log, nunca a la respuesta.
	@ExceptionHandler(Exception.class)
	ProblemDetail inesperado(Exception ex) {
		log.error("Error no previsto", ex);
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"Se ha producido un error inesperado.");
		problema.setTitle("Error interno");
		problema.setProperty("codigo", "ERROR_INTERNO");
		return problema;
	}

	// Los errores que Spring ya sabe resolver (ruta inexistente, método no permitido, JSON mal formado…)
	// pasan por aquí: se les añade el campo "codigo" para que todos los errores tengan el mismo formato.
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		ResponseEntity<Object> respuesta = super.handleExceptionInternal(ex, body, headers, statusCode, request);
		if (respuesta != null && respuesta.getBody() instanceof ProblemDetail problema
				&& (problema.getProperties() == null || !problema.getProperties().containsKey("codigo"))) {
			problema.setProperty("codigo", codigoDe(statusCode));
		}
		return respuesta;
	}

	// Un cuerpo que incumple las anotaciones de validación (@NotBlank, @Size…): 400 con la lista de campos erróneos.
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
		List<ErrorDeCampo> errores = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> new ErrorDeCampo(e.getField(), e.getDefaultMessage())).toList();
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Hay datos que no son válidos.");
		problema.setTitle("Datos no válidos");
		problema.setProperty("codigo", "VALIDACION");
		problema.setProperty("errores", errores);
		return handleExceptionInternal(ex, problema, headers, statusCode, request);
	}

	private record ErrorDeCampo(String campo, String mensaje) {
	}


	private static ProblemDetail problema(HttpStatus estado, String titulo, ErrorDeDominio ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, ex.getMessage());
		problema.setTitle(titulo);
		problema.setProperty("codigo", ex.getCodigo());
		return problema;
	}

	private static String codigoDe(HttpStatusCode estado) {
		return switch (estado.value()) {
		case 400 -> "VALIDACION";
		case 404 -> "NO_ENCONTRADO";
		case 405 -> "METODO_NO_PERMITIDO";
		default -> "ERROR_PETICION";
		};
	}

}