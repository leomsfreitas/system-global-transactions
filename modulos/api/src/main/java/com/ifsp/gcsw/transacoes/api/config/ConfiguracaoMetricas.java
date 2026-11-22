package com.ifsp.gcsw.transacoes.api.config;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

/**
 * Exporta metricas Micrometer para o CloudWatch (perfil aws). Credenciais via role da tarefa ECS.
 */
@Configuration
@ConditionalOnProperty(name = "metricas.cloudwatch.habilitado", havingValue = "true")
public class ConfiguracaoMetricas {

  @Bean
  CloudWatchMeterRegistry cloudWatchMeterRegistry(
      @Value("${metricas.cloudwatch.namespace:TransacoesGeopoliticas}") String namespace,
      @Value("${metricas.cloudwatch.intervalo:PT1M}") String intervalo) {
    Map<String, String> config =
        Map.of("cloudwatch.namespace", namespace, "cloudwatch.step", intervalo);
    CloudWatchConfig cloudWatchConfig = config::get;
    return new CloudWatchMeterRegistry(
        cloudWatchConfig, Clock.SYSTEM, CloudWatchAsyncClient.create());
  }
}
