package com.ifsp.gcsw.transacoes.conectores.http;

/** Fonte fora do ar apos as tentativas, com timeout ou com circuito aberto. */
public class FonteIndisponivelException extends RuntimeException {
  public FonteIndisponivelException(String mensagem, Throwable causa) {
    super(mensagem, causa);
  }
}
