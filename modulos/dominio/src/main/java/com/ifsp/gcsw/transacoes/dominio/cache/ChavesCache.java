package com.ifsp.gcsw.transacoes.dominio.cache;

/** Chaves Redis da secao 6.4. */
public final class ChavesCache {

  public static final String PREFIXO_COTACAO = "cotacao:atual:";
  public static final String PREFIXO_INDICE = "indice:atual:";
  public static final String PREFIXO_INDICADORES = "indicadores:";
  public static final String MOEDAS = "moedas:lista";

  private ChavesCache() {}

  public static String cotacaoAtual(String base, String destino) {
    return PREFIXO_COTACAO + base + ":" + destino;
  }

  public static String indiceAtual(String pais) {
    return PREFIXO_INDICE + pais;
  }

  public static String indicadores(String pais) {
    return PREFIXO_INDICADORES + pais;
  }
}
