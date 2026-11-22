package com.ifsp.gcsw.transacoes.conectores.http;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * GET com timeout, retry com backoff exponencial so para erros transitorios (5xx, timeout, falha de
 * conexao) e circuit breaker por fonte (secao 6.5).
 */
public class ClienteHttpResiliente {

  private static final Logger LOG = LoggerFactory.getLogger(ClienteHttpResiliente.class);

  private final String nome;
  private final WebClient webClient;
  private final CircuitBreaker circuito;
  private final Retry retry;
  private final Duration timeoutTotal;

  public ClienteHttpResiliente(
      String nome,
      WebClient webClient,
      CircuitBreaker circuito,
      Retry retry,
      Duration timeoutTotal) {
    this.nome = nome;
    this.webClient = webClient;
    this.circuito = circuito;
    this.retry = retry;
    this.timeoutTotal = timeoutTotal;
  }

  public String nome() {
    return nome;
  }

  public RespostaHttp get(URI uri) {
    return get(uri, Map.of());
  }

  public RespostaHttp get(URI uri, Map<String, String> cabecalhos) {
    Mono<RespostaHttp> chamada =
        webClient
            .get()
            .uri(uri)
            .headers(h -> cabecalhos.forEach(h::set))
            .exchangeToMono(resposta -> {
              int status = resposta.statusCode().value();
              return resposta
                  .bodyToMono(String.class)
                  .defaultIfEmpty("")
                  .flatMap(corpo ->
                      status >= 500
                          ? Mono.error(new ErroTransitorioException("HTTP " + status))
                          : Mono.just(new RespostaHttp(status, corpo)));
            })
            .timeout(timeoutTotal)
            .transformDeferred(CircuitBreakerOperator.of(circuito))
            .transformDeferred(RetryOperator.of(retry));
    try {
      return chamada.block();
    } catch (CallNotPermittedException e) {
      LOG.warn("Circuito aberto para a fonte {}: chamada nao realizada", nome);
      throw new FonteIndisponivelException("Circuito aberto para a fonte " + nome, e);
    } catch (RuntimeException e) {
      Throwable causa = reactor.core.Exceptions.unwrap(e);
      LOG.warn("Fonte {} indisponivel em {}: {}", nome, uri.getHost(), causa.toString());
      throw new FonteIndisponivelException(
          "Fonte " + nome + " indisponivel: " + causa.getMessage(), causa);
    }
  }
}
