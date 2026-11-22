package com.ifsp.gcsw.transacoes.dominio.cache;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/** Usado quando cache.habilitado=false: sempre consulta a origem. */
public class CacheDesligado implements CacheConsultas {

  @Override
  public <T> T obter(String chave, Class<T> tipo, Duration ttl, Supplier<T> carregador) {
    return carregador.get();
  }

  @Override
  public <T> List<T> obterLista(
      String chave, Class<T> tipoElemento, Duration ttl, Supplier<List<T>> carregador) {
    return carregador.get();
  }

  @Override
  public void invalidar(String chave) {}

  @Override
  public void invalidarPorPrefixo(String prefixo) {}
}
