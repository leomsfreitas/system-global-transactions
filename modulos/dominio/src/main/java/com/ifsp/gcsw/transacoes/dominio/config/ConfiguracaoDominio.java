package com.ifsp.gcsw.transacoes.dominio.config;

import com.ifsp.gcsw.transacoes.dominio.cache.CacheConsultas;
import com.ifsp.gcsw.transacoes.dominio.cache.CacheDesligado;
import com.ifsp.gcsw.transacoes.dominio.cache.CacheRedis;
import com.ifsp.gcsw.transacoes.dominio.estrategia.EstrategiaAjusteGeopolitico;
import com.ifsp.gcsw.transacoes.dominio.motor.CalculadoraIndiceGeopolitico;
import com.ifsp.gcsw.transacoes.dominio.motor.MotorConversao;
import com.ifsp.gcsw.transacoes.dominio.taxa.ConsultaCotacoes;
import com.ifsp.gcsw.transacoes.dominio.taxa.ResolvedorTaxaCambio;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties({
  PropriedadesAjusteGeopolitico.class,
  PropriedadesCotacao.class,
  PropriedadesCache.class
})
public class ConfiguracaoDominio {

  @Bean
  @ConditionalOnMissingBean
  Clock relogio() {
    return Clock.systemUTC();
  }

  @Bean
  MotorConversao motorConversao() {
    return new MotorConversao();
  }

  @Bean
  CalculadoraIndiceGeopolitico calculadoraIndiceGeopolitico(PropriedadesAjusteGeopolitico props) {
    return new CalculadoraIndiceGeopolitico(props.parametros());
  }

  @Bean
  EstrategiaAjusteGeopolitico estrategiaAjusteGeopolitico(PropriedadesAjusteGeopolitico props) {
    return props.estrategia().criar();
  }

  @Bean
  ResolvedorTaxaCambio resolvedorTaxaCambio(
      ConsultaCotacoes consulta, PropriedadesCotacao props, Clock relogio) {
    return new ResolvedorTaxaCambio(consulta, props.pivos(), props.idadeMaxima(), relogio);
  }

  @Bean
  @ConditionalOnMissingBean
  CacheConsultas cacheConsultas(
      PropriedadesCache props,
      ObjectProvider<StringRedisTemplate> redis,
      JsonMapper json,
      Clock relogio,
      MeterRegistry metricas) {
    StringRedisTemplate template = redis.getIfAvailable();
    if (!props.habilitado() || template == null) {
      return new CacheDesligado();
    }
    return new CacheRedis(template, json, relogio, props.pausaAposFalha(), metricas);
  }
}
