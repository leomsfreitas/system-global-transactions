package com.ifsp.gcsw.transacoes.dominio.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

class CacheRedisTest {

  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

  @SuppressWarnings("unchecked")
  private final ValueOperations<String, String> ops = mock(ValueOperations.class);

  private final SimpleMeterRegistry metricas = new SimpleMeterRegistry();
  private final Clock relogio = Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC);
  private final CacheRedis cache =
      new CacheRedis(
          redis, JsonMapper.builder().build(), relogio, Duration.ofSeconds(30), metricas);

  record Valor(String nome, int numero) {}

  @Test
  void redisFora_usaCarregadorEPausaORedis() {
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.get(anyString())).thenThrow(new RedisConnectionFailureException("fora"));

    Valor v1 = cache.obter("k", Valor.class, Duration.ofMinutes(1), () -> new Valor("banco", 1));
    Valor v2 = cache.obter("k", Valor.class, Duration.ofMinutes(1), () -> new Valor("banco", 2));

    assertThat(v1.nome()).isEqualTo("banco");
    assertThat(v2.numero()).isEqualTo(2);
    // Segunda chamada nem tenta o Redis: pausado apos a falha.
    verify(ops, times(1)).get("k");
    assertThat(metricas.counter("cache.acessos", "resultado", "erro").count()).isEqualTo(1);
  }

  @Test
  void acertoDesserializaJson() {
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.get("k")).thenReturn("{\"nome\":\"cache\",\"numero\":7}");

    Valor v = cache.obter("k", Valor.class, Duration.ofMinutes(1), () -> new Valor("banco", 1));

    assertThat(v).isEqualTo(new Valor("cache", 7));
    verify(ops, never())
        .set(anyString(), anyString(), org.mockito.ArgumentMatchers.any(Duration.class));
  }

  @Test
  void listaRoundTrip() {
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.get("l")).thenReturn("[{\"nome\":\"a\",\"numero\":1}]");
    List<Valor> lista = cache.obterLista("l", Valor.class, Duration.ofMinutes(1), List::of);
    assertThat(lista).containsExactly(new Valor("a", 1));
  }

  @Test
  void falhaNaEscritaEIgnorada() {
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.get("k")).thenReturn(null);
    org.mockito.Mockito.doThrow(new RedisConnectionFailureException("fora"))
        .when(ops)
        .set(anyString(), anyString(), org.mockito.ArgumentMatchers.any(Duration.class));

    assertThat(
            cache
                .obter("k", Valor.class, Duration.ofMinutes(1), () -> new Valor("banco", 1))
                .nome())
        .isEqualTo("banco");
  }

  @Test
  void invalidacaoComRedisForaNaoPropagaErro() {
    when(redis.delete("k")).thenThrow(new RedisConnectionFailureException("fora"));
    cache.invalidar("k");
    cache.invalidarPorPrefixo("cotacao:");
    verify(redis, never()).scan(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void erroDoCarregadorPropaga() {
    when(redis.opsForValue()).thenReturn(ops);
    assertThatThrownBy(() ->
            cache.obter(
                "k",
                Valor.class,
                Duration.ofMinutes(1),
                () -> {
                  throw new IllegalStateException("x");
                }))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void stampede_requisicoesSimultaneasRecomputamUmaVez() throws Exception {
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.get("quente")).thenReturn(null);
    AtomicInteger chamadas = new AtomicInteger();
    CountDownLatch liberar = new CountDownLatch(1);
    int threads = 16;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      List<Future<Valor>> futuros =
          java.util.stream.IntStream.range(0, threads)
              .mapToObj(i ->
                  pool.submit(() ->
                      cache.obter(
                          "quente",
                          Valor.class,
                          Duration.ofMinutes(1),
                          () -> {
                            chamadas.incrementAndGet();
                            try {
                              liberar.await(2, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                              Thread.currentThread().interrupt();
                            }
                            return new Valor("db", 1);
                          })))
              .toList();
      Thread.sleep(200);
      liberar.countDown();
      for (Future<Valor> f : futuros) {
        assertThat(f.get(5, TimeUnit.SECONDS).nome()).isEqualTo("db");
      }
    } finally {
      pool.shutdownNow();
    }
    assertThat(chamadas.get()).isEqualTo(1);
  }

  @Test
  void cacheDesligadoSempreCarrega() {
    CacheDesligado desligado = new CacheDesligado();
    assertThat(desligado.obter("k", String.class, Duration.ZERO, () -> "x")).isEqualTo("x");
    assertThat(desligado.obterLista("k", String.class, Duration.ZERO, () -> List.of("y")))
        .containsExactly("y");
    desligado.invalidar("k");
    desligado.invalidarPorPrefixo("k");
  }
}
