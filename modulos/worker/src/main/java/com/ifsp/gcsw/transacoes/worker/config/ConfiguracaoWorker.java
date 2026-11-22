package com.ifsp.gcsw.transacoes.worker.config;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

@Configuration
@EnableConfigurationProperties(PropriedadesWorker.class)
public class ConfiguracaoWorker {

  /** Metricas no CloudWatch (perfil aws). Publicadas tambem no encerramento da tarefa. */
  @Bean
  @ConditionalOnProperty(name = "metricas.cloudwatch.habilitado", havingValue = "true")
  CloudWatchMeterRegistry cloudWatchMeterRegistry(
      @Value("${metricas.cloudwatch.namespace:TransacoesGeopoliticas}") String namespace) {
    Map<String, String> config =
        Map.of("cloudwatch.namespace", namespace, "cloudwatch.step", "PT1M");
    CloudWatchConfig cloudWatchConfig = config::get;
    return new CloudWatchMeterRegistry(
        cloudWatchConfig, Clock.SYSTEM, CloudWatchAsyncClient.create());
  }
}
