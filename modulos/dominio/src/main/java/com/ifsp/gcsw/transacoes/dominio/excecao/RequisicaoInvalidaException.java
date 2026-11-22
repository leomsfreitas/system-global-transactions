package com.ifsp.gcsw.transacoes.dominio.excecao;

/** Regra de negocio violada pela entrada (ex.: intervalo do historico): HTTP 400. */
public class RequisicaoInvalidaException extends RuntimeException {
  public RequisicaoInvalidaException(String mensagem) {
    super(mensagem);
  }
}
