package com.ifsp.gcsw.transacoes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** API REST (pacote raiz para que entidades e repositorios do modulo dominio sejam encontrados). */
@SpringBootApplication
public class AplicacaoApi {

  public static void main(String[] args) {
    SpringApplication.run(AplicacaoApi.class, args);
  }
}
