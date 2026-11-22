package com.ifsp.gcsw.transacoes.dominio.excecao;

/** Moeda, pais ou dado inexistente: HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {
  public RecursoNaoEncontradoException(String mensagem) {
    super(mensagem);
  }
}
