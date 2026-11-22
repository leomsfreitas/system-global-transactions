package com.ifsp.gcsw.transacoes.conectores.http;

/** Resposta 2xx ou 4xx (5xx e timeouts viram excecao apos as tentativas). */
public record RespostaHttp(int status, String corpo) {

  public boolean sucesso() {
    return status >= 200 && status < 300;
  }

  public boolean corpoVazio() {
    return corpo == null || corpo.isBlank();
  }
}
