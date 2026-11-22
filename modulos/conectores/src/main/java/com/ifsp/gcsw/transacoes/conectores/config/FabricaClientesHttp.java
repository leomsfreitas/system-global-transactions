package com.ifsp.gcsw.transacoes.conectores.config;

import com.ifsp.gcsw.transacoes.conectores.http.ClienteHttpResiliente;
import com.ifsp.gcsw.transacoes.conectores.http.ErroTransitorioException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.netty.channel.ChannelOption;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.netty.http.client.HttpClient;

/** Monta um cliente HTTP resiliente por fonte, com circuito e retry proprios. */
public class FabricaClientesHttp {

  /** Erros transitorios: 5xx, timeout e falha de conexao. 4xx nao entram (nao adianta repetir). */
  static final Predicate<Throwable> TRANSITORIO =
      e ->
          e instanceof ErroTransitorioException
              || e instanceof TimeoutException
              || e instanceof WebClientRequestException
              || e instanceof IOException;

  private final PropriedadesConectores props;
  private final WebClient.Builder construtor;
  private final CircuitBreakerRegistry circuitos;
  private final RetryRegistry retries;

  public FabricaClientesHttp(
      PropriedadesConectores props,
      WebClient.Builder construtor,
      CircuitBreakerRegistry circuitos,
      RetryRegistry retries) {
    this.props = props;
    this.construtor = construtor;
    this.circuitos = circuitos;
    this.retries = retries;
  }

  public static CircuitBreakerRegistry registroCircuitos(PropriedadesConectores props) {
    return CircuitBreakerRegistry.of(
        CircuitBreakerConfig.custom()
            .slidingWindowSize(props.janelaCircuito())
            .minimumNumberOfCalls(Math.min(props.janelaCircuito(), 5))
            .failureRateThreshold(props.percentualFalhasCircuito())
            .waitDurationInOpenState(props.esperaCircuitoAberto())
            .recordException(TRANSITORIO)
            .build());
  }

  public static RetryRegistry registroRetries(PropriedadesConectores props) {
    return RetryRegistry.of(
        RetryConfig.custom()
            .maxAttempts(props.tentativas())
            .intervalFunction(
                IntervalFunction.ofExponentialBackoff(
                    props.esperaInicial(), props.multiplicadorEspera()))
            .retryOnException(TRANSITORIO)
            .build());
  }

  public ClienteHttpResiliente criar(String nome) {
    HttpClient http =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) props.timeoutConexao().toMillis())
            .responseTimeout(props.timeoutLeitura())
            .followRedirect(true);
    WebClient webClient =
        construtor
            .clone()
            .clientConnector(new ReactorClientHttpConnector(http))
            .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .defaultHeader("User-Agent", "transacoes-geopoliticas/1.0")
            .build();
    Duration total = props.timeoutConexao().plus(props.timeoutLeitura());
    return new ClienteHttpResiliente(
        nome, webClient, circuitos.circuitBreaker(nome), retries.retry(nome), total);
  }
}
