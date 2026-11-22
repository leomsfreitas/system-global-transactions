package com.ifsp.gcsw.transacoes.conectores.http;

/** A fonte respondeu, mas o conteudo nao pode ser interpretado (JSON malformado, erro no corpo). */
public class RespostaInvalidaException extends RuntimeException {
  public RespostaInvalidaException(String mensagem, Throwable causa) {
    super(mensagem, causa);
  }
}
