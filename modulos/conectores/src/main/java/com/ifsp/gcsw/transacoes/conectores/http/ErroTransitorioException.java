package com.ifsp.gcsw.transacoes.conectores.http;

/** 5xx: elegivel para retry. */
public class ErroTransitorioException extends RuntimeException {
  public ErroTransitorioException(String mensagem) {
    super(mensagem);
  }
}
