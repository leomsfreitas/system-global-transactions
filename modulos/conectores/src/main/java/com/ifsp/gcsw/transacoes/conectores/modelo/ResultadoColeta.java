package com.ifsp.gcsw.transacoes.conectores.modelo;

import java.util.List;

/** Itens validos e quantos foram descartados pela validacao (secao 6.5). */
public record ResultadoColeta<T>(List<T> itens, int descartados) {

  public static <T> ResultadoColeta<T> vazio() {
    return new ResultadoColeta<>(List.of(), 0);
  }

  public int lidos() {
    return itens.size() + descartados;
  }
}
