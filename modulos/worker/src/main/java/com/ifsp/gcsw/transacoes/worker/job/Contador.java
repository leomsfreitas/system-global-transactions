package com.ifsp.gcsw.transacoes.worker.job;

/** Acumula gravados e ignorados a partir do retorno 1/0 do INSERT ... ON CONFLICT DO NOTHING. */
final class Contador {
  int gravados;
  int ignorados;

  void registrar(int linhas) {
    if (linhas > 0) {
      gravados++;
    } else {
      ignorados++;
    }
  }
}
