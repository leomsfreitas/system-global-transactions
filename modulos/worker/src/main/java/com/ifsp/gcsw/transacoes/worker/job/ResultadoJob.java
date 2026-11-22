package com.ifsp.gcsw.transacoes.worker.job;

/** Contadores de uma execucao (secao 6.4). ignorados = ja existiam (ON CONFLICT DO NOTHING). */
public record ResultadoJob(int lidos, int gravados, int ignorados, int descartados, int falhas) {

  public static final ResultadoJob VAZIO = new ResultadoJob(0, 0, 0, 0, 0);

  public ResultadoJob somar(ResultadoJob outro) {
    return new ResultadoJob(
        lidos + outro.lidos,
        gravados + outro.gravados,
        ignorados + outro.ignorados,
        descartados + outro.descartados,
        falhas + outro.falhas);
  }

  public static ResultadoJob falha() {
    return new ResultadoJob(0, 0, 0, 0, 1);
  }
}
