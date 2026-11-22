package com.ifsp.gcsw.transacoes.api.web.dto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/** Corpo de erro padrao: { "codigo": 400, "mensagem": "...", "dataHora": "..." }. */
public record Erro(int codigo, String mensagem, OffsetDateTime dataHora) {

  public static Erro de(int codigo, String mensagem) {
    return new Erro(
        codigo, mensagem, OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS));
  }
}
