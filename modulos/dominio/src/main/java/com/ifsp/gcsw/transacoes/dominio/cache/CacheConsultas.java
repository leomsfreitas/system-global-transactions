package com.ifsp.gcsw.transacoes.dominio.cache;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Cache de consultas. E otimizacao, nao dependencia (secao 6.4): se o cache falhar, o carregador e
 * chamado e a resposta sai do PostgreSQL.
 */
public interface CacheConsultas {

  <T> T obter(String chave, Class<T> tipo, Duration ttl, Supplier<T> carregador);

  <T> List<T> obterLista(
      String chave, Class<T> tipoElemento, Duration ttl, Supplier<List<T>> carregador);

  void invalidar(String chave);

  void invalidarPorPrefixo(String prefixo);
}
