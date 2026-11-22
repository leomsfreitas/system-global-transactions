package com.ifsp.gcsw.transacoes.worker.job;

/** Um job do worker. Toda execucao precisa ser idempotente. */
public interface Job {

  String nome();

  ResultadoJob executar();
}
