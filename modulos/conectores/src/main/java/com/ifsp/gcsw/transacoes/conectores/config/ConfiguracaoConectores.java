package com.ifsp.gcsw.transacoes.conectores.config;

import com.ifsp.gcsw.transacoes.conectores.bancomundial.BancoMundialCliente;
import com.ifsp.gcsw.transacoes.conectores.bancomundial.RiscoPoliticoBancoMundial;
import com.ifsp.gcsw.transacoes.conectores.bcb.BcbPtaxCliente;
import com.ifsp.gcsw.transacoes.conectores.bcb.BcbSgsCliente;
import com.ifsp.gcsw.transacoes.conectores.bce.BceCliente;
import com.ifsp.gcsw.transacoes.conectores.fmi.FmiCliente;
import com.ifsp.gcsw.transacoes.conectores.http.ValidadorDados;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(PropriedadesConectores.class)
public class ConfiguracaoConectores {

  @Bean
  @ConditionalOnMissingBean
  Clock relogioConectores() {
    return Clock.systemUTC();
  }

  @Bean
  CircuitBreakerRegistry registroCircuitos(PropriedadesConectores props, MeterRegistry metricas) {
    CircuitBreakerRegistry registro = FabricaClientesHttp.registroCircuitos(props);
    TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registro).bindTo(metricas);
    return registro;
  }

  @Bean
  RetryRegistry registroRetries(PropriedadesConectores props, MeterRegistry metricas) {
    RetryRegistry registro = FabricaClientesHttp.registroRetries(props);
    TaggedRetryMetrics.ofRetryRegistry(registro).bindTo(metricas);
    return registro;
  }

  @Bean
  FabricaClientesHttp fabricaClientesHttp(
      PropriedadesConectores props,
      WebClient.Builder construtor,
      CircuitBreakerRegistry circuitos,
      RetryRegistry retries) {
    return new FabricaClientesHttp(props, construtor, circuitos, retries);
  }

  @Bean
  ValidadorDados validadorDados(Clock relogio) {
    return new ValidadorDados(relogio);
  }

  @Bean
  BcbPtaxCliente bcbPtaxCliente(
      FabricaClientesHttp fabrica, PropriedadesConectores props, ValidadorDados v) {
    return new BcbPtaxCliente(fabrica.criar("BCB-PTAX"), props.urls().bcbPtax(), v);
  }

  @Bean
  BcbSgsCliente bcbSgsCliente(
      FabricaClientesHttp fabrica, PropriedadesConectores props, ValidadorDados v, Clock relogio) {
    return new BcbSgsCliente(
        fabrica.criar("BCB-SGS"), props.urls().bcbSgs(), v, relogio, props.mesesHistoricoSgs());
  }

  @Bean
  BceCliente bceCliente(
      FabricaClientesHttp fabrica, PropriedadesConectores props, ValidadorDados v) {
    return new BceCliente(fabrica.criar("BCE"), props.urls().bce(), v);
  }

  @Bean
  BancoMundialCliente bancoMundialCliente(
      FabricaClientesHttp fabrica, PropriedadesConectores props, ValidadorDados v, Clock relogio) {
    return new BancoMundialCliente(
        fabrica.criar("BANCO_MUNDIAL"),
        props.urls().bancoMundial(),
        props.indicadorEstabilidadePolitica(),
        v,
        relogio,
        props.anosHistoricoIndicadores());
  }

  @Bean
  RiscoPoliticoBancoMundial riscoPoliticoBancoMundial(BancoMundialCliente cliente) {
    return new RiscoPoliticoBancoMundial(cliente);
  }

  @Bean
  FmiCliente fmiCliente(
      FabricaClientesHttp fabrica, PropriedadesConectores props, ValidadorDados v, Clock relogio) {
    return new FmiCliente(
        fabrica.criar("FMI"), props.urls().fmi(), v, relogio, props.anosHistoricoIndicadores());
  }
}
