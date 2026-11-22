package com.ifsp.gcsw.transacoes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servico de atualizacao. Na AWS cada execucao e uma tarefa ECS disparada pelo EventBridge com
 * --job=NOME, que roda o job e encerra. No perfil local os jobs rodam por @Scheduled.
 */
@SpringBootApplication
public class AplicacaoWorker {

  public static void main(String[] args) {
    SpringApplication.run(AplicacaoWorker.class, args);
  }
}
