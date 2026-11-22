package com.ifsp.gcsw.transacoes.dominio.excecao;

/** Nao ha dado para atender (ex.: par sem cotacao em nenhuma fonte): HTTP 503. */
public class DadoIndisponivelException extends RuntimeException {
  public DadoIndisponivelException(String mensagem) {
    super(mensagem);
  }
}
