package com.ifsp.gcsw.transacoes.dominio.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cache em Redis com valores JSON.
 *
 * <ul>
 *   <li>Falha do Redis nunca vira erro para o cliente: o carregador e chamado (fallback ao banco).
 *   <li>Apos uma falha, o Redis e ignorado por {@code pausaAposFalha}, para nao somar o timeout a
 *       cada requisicao enquanto ele estiver fora.
 *   <li>Protecao contra cache stampede: requisicoes simultaneas para a mesma chave ausente aguardam
 *       uma unica recomputacao (single flight por instancia).
 * </ul>
 */
public class CacheRedis implements CacheConsultas {

  private static final Logger LOG = LoggerFactory.getLogger(CacheRedis.class);

  private final StringRedisTemplate redis;
  private final JsonMapper json;
  private final Clock relogio;
  private final Duration pausaAposFalha;
  private final ConcurrentHashMap<String, CompletableFuture<Object>> emAndamento =
      new ConcurrentHashMap<>();
  private final Counter acertos;
  private final Counter faltas;
  private final Counter erros;
  private volatile long ignorarAte;

  public CacheRedis(
      StringRedisTemplate redis,
      JsonMapper json,
      Clock relogio,
      Duration pausaAposFalha,
      MeterRegistry metricas) {
    this.redis = redis;
    this.json = json;
    this.relogio = relogio;
    this.pausaAposFalha = pausaAposFalha;
    this.acertos = Counter.builder("cache.acessos").tag("resultado", "acerto").register(metricas);
    this.faltas = Counter.builder("cache.acessos").tag("resultado", "falta").register(metricas);
    this.erros = Counter.builder("cache.acessos").tag("resultado", "erro").register(metricas);
  }

  @Override
  public <T> T obter(String chave, Class<T> tipo, Duration ttl, Supplier<T> carregador) {
    return obterTipado(chave, json.constructType(tipo), ttl, carregador);
  }

  @Override
  public <T> List<T> obterLista(
      String chave, Class<T> tipoElemento, Duration ttl, Supplier<List<T>> carregador) {
    JavaType tipo = json.getTypeFactory().constructCollectionType(List.class, tipoElemento);
    return obterTipado(chave, tipo, ttl, carregador);
  }

  @Override
  public void invalidar(String chave) {
    if (redisPausado()) {
      return;
    }
    try {
      redis.delete(chave);
    } catch (RuntimeException e) {
      registrarFalha("invalidar", e);
    }
  }

  @Override
  public void invalidarPorPrefixo(String prefixo) {
    if (redisPausado()) {
      return;
    }
    try {
      List<String> chaves = new ArrayList<>();
      ScanOptions opcoes = ScanOptions.scanOptions().match(prefixo + "*").count(500).build();
      try (Cursor<String> cursor = redis.scan(opcoes)) {
        cursor.forEachRemaining(chaves::add);
      }
      if (!chaves.isEmpty()) {
        redis.delete(chaves);
      }
    } catch (RuntimeException e) {
      registrarFalha("invalidarPorPrefixo", e);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T obterTipado(String chave, JavaType tipo, Duration ttl, Supplier<T> carregador) {
    T emCache = ler(chave, tipo);
    if (emCache != null) {
      acertos.increment();
      return emCache;
    }
    faltas.increment();
    CompletableFuture<Object> minha = new CompletableFuture<>();
    CompletableFuture<Object> existente = emAndamento.putIfAbsent(chave, minha);
    if (existente != null) {
      return (T) aguardar(existente);
    }
    try {
      T valor = carregador.get();
      escrever(chave, valor, ttl);
      minha.complete(valor);
      return valor;
    } catch (RuntimeException e) {
      minha.completeExceptionally(e);
      throw e;
    } finally {
      emAndamento.remove(chave, minha);
    }
  }

  private static Object aguardar(CompletableFuture<Object> futuro) {
    try {
      return futuro.join();
    } catch (CompletionException e) {
      if (e.getCause() instanceof RuntimeException causa) {
        throw causa;
      }
      throw e;
    }
  }

  private <T> T ler(String chave, JavaType tipo) {
    if (redisPausado()) {
      return null;
    }
    try {
      String valor = redis.opsForValue().get(chave);
      return valor == null ? null : json.readValue(valor, tipo);
    } catch (RuntimeException e) {
      registrarFalha("ler", e);
      return null;
    }
  }

  private void escrever(String chave, Object valor, Duration ttl) {
    if (valor == null || redisPausado()) {
      return;
    }
    try {
      redis.opsForValue().set(chave, json.writeValueAsString(valor), ttl);
    } catch (RuntimeException e) {
      registrarFalha("escrever", e);
    }
  }

  private boolean redisPausado() {
    return relogio.millis() < ignorarAte;
  }

  private void registrarFalha(String operacao, RuntimeException e) {
    erros.increment();
    ignorarAte = relogio.millis() + pausaAposFalha.toMillis();
    LOG.warn(
        "Cache Redis indisponivel na operacao {}; servindo pelo banco por {} s: {}",
        operacao,
        pausaAposFalha.toSeconds(),
        e.getMessage());
  }
}
