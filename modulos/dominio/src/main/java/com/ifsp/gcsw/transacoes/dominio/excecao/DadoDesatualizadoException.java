package com.ifsp.gcsw.transacoes.dominio.excecao;

import java.time.OffsetDateTime;

/** A cotacao mais recente e mais antiga que cotacao.idade-maxima: HTTP 503 (secao 5.6). */
public class DadoDesatualizadoException extends DadoIndisponivelException {

  private final transient OffsetDateTime dataReferencia;

  public DadoDesatualizadoException(String mensagem, OffsetDateTime dataReferencia) {
    super(mensagem);
    this.dataReferencia = dataReferencia;
  }

  public OffsetDateTime getDataReferencia() {
    return dataReferencia;
  }
}
