package com.ifsp.gcsw.transacoes.api.web;

import com.ifsp.gcsw.transacoes.api.web.dto.Erro;
import com.ifsp.gcsw.transacoes.dominio.excecao.DadoIndisponivelException;
import com.ifsp.gcsw.transacoes.dominio.excecao.RecursoNaoEncontradoException;
import com.ifsp.gcsw.transacoes.dominio.excecao.RequisicaoInvalidaException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Mapeia excecoes para o corpo Erro (secao 6.6). Nunca devolve stack trace. */
@RestControllerAdvice
public class TratadorErros {

  private static final Logger LOG = LoggerFactory.getLogger(TratadorErros.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Erro> corpoInvalido(MethodArgumentNotValidException e) {
    String mensagem =
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .sorted()
            .collect(Collectors.joining("; "));
    return responder(HttpStatus.BAD_REQUEST, mensagem);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<Erro> parametroInvalido(HandlerMethodValidationException e) {
    String mensagem =
        e.getParameterValidationResults().stream()
            .flatMap(r ->
                r.getResolvableErrors().stream()
                    .map(erro ->
                        r.getMethodParameter().getParameterName()
                            + ": "
                            + erro.getDefaultMessage()))
            .sorted()
            .collect(Collectors.joining("; "));
    return responder(HttpStatus.BAD_REQUEST, mensagem);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<Erro> restricaoViolada(ConstraintViolationException e) {
    String mensagem =
        e.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .sorted()
            .collect(Collectors.joining("; "));
    return responder(HttpStatus.BAD_REQUEST, mensagem);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Erro> jsonInvalido(HttpMessageNotReadableException e) {
    return responder(
        HttpStatus.BAD_REQUEST,
        "Corpo da requisicao invalido: verifique o JSON, os tipos e os campos aceitos");
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<Erro> parametroAusente(MissingServletRequestParameterException e) {
    return responder(
        HttpStatus.BAD_REQUEST, "Parametro obrigatorio ausente: " + e.getParameterName());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<Erro> tipoInvalido(MethodArgumentTypeMismatchException e) {
    return responder(HttpStatus.BAD_REQUEST, "Valor invalido para o parametro " + e.getName());
  }

  @ExceptionHandler(RequisicaoInvalidaException.class)
  ResponseEntity<Erro> regraViolada(RequisicaoInvalidaException e) {
    return responder(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(RecursoNaoEncontradoException.class)
  ResponseEntity<Erro> naoEncontrado(RecursoNaoEncontradoException e) {
    return responder(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<Erro> rotaInexistente(NoResourceFoundException e) {
    return responder(HttpStatus.NOT_FOUND, "Recurso nao encontrado");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<Erro> metodo(HttpRequestMethodNotSupportedException e) {
    return responder(HttpStatus.METHOD_NOT_ALLOWED, "Metodo nao permitido");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<Erro> tipoMidia(HttpMediaTypeNotSupportedException e) {
    return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use Content-Type: application/json");
  }

  @ExceptionHandler(DadoIndisponivelException.class)
  ResponseEntity<Erro> indisponivel(DadoIndisponivelException e) {
    return responder(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
  }

  @ExceptionHandler(DataAccessException.class)
  ResponseEntity<Erro> banco(DataAccessException e) {
    LOG.error("Falha de acesso ao banco: {}", e.getMostSpecificCause().getMessage());
    return responder(HttpStatus.SERVICE_UNAVAILABLE, "Servico temporariamente indisponivel");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Erro> generico(Exception e) {
    LOG.error("Erro nao tratado", e);
    return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
  }

  private static ResponseEntity<Erro> responder(HttpStatus status, String mensagem) {
    return ResponseEntity.status(status).body(Erro.de(status.value(), mensagem));
  }
}
