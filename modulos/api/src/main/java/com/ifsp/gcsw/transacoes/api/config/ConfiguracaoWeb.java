package com.ifsp.gcsw.transacoes.api.config;

import com.ifsp.gcsw.transacoes.api.seguranca.FiltroCabecalhosSeguranca;
import com.ifsp.gcsw.transacoes.api.seguranca.FiltroChaveApi;
import com.ifsp.gcsw.transacoes.api.seguranca.FiltroLimiteRequisicoes;
import com.ifsp.gcsw.transacoes.api.seguranca.FiltroRastreamento;
import com.ifsp.gcsw.transacoes.api.seguranca.PropriedadesSeguranca;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(PropriedadesSeguranca.class)
public class ConfiguracaoWeb {

  /**
   * Decimais sem notacao cientifica e rejeicao de campos desconhecidos (additionalProperties:
   * false).
   */
  @Bean
  JsonMapperBuilderCustomizer jsonDecimaisERigor() {
    return builder ->
        builder
            .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
  }

  @Bean
  FilterRegistrationBean<FiltroRastreamento> filtroRastreamento() {
    return registrar(new FiltroRastreamento(), Ordered.HIGHEST_PRECEDENCE);
  }

  @Bean
  FilterRegistrationBean<FiltroCabecalhosSeguranca> filtroCabecalhosSeguranca() {
    return registrar(new FiltroCabecalhosSeguranca(), Ordered.HIGHEST_PRECEDENCE + 1);
  }

  @Bean
  FilterRegistrationBean<FiltroChaveApi> filtroChaveApi(
      PropriedadesSeguranca props, JsonMapper json) {
    return registrar(new FiltroChaveApi(props, json), Ordered.HIGHEST_PRECEDENCE + 2);
  }

  @Bean
  FilterRegistrationBean<FiltroLimiteRequisicoes> filtroLimiteRequisicoes(
      PropriedadesSeguranca props, JsonMapper json) {
    FilterRegistrationBean<FiltroLimiteRequisicoes> registro =
        registrar(
            new FiltroLimiteRequisicoes(props.limiteRequisicoes(), json),
            Ordered.HIGHEST_PRECEDENCE + 3);
    registro.setEnabled(props.limiteRequisicoes().habilitado());
    return registro;
  }

  private static <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> registrar(
      T filtro, int ordem) {
    FilterRegistrationBean<T> registro = new FilterRegistrationBean<>(filtro);
    registro.setOrder(ordem);
    registro.addUrlPatterns("/*");
    return registro;
  }
}
