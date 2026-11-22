package com.ifsp.gcsw.transacoes.api.seguranca;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * seguranca.* (secao 5.7). As chaves chegam como hashes SHA-256 em hexadecimal, vindos do Secrets
 * Manager na AWS ou de variavel de ambiente localmente. A chave em texto claro nunca e configurada.
 */
@ConfigurationProperties("seguranca")
public record PropriedadesSeguranca(
    List<String> chavesApiSha256, LimiteRequisicoes limiteRequisicoes) {

  public PropriedadesSeguranca {
    chavesApiSha256 = chavesApiSha256 == null ? List.of() : chavesApiSha256;
    limiteRequisicoes =
        limiteRequisicoes == null ? new LimiteRequisicoes(false, 50, 100) : limiteRequisicoes;
  }

  public record LimiteRequisicoes(
      @DefaultValue("false") boolean habilitado,
      @DefaultValue("50") long requisicoesPorSegundo,
      @DefaultValue("100") long rajada) {}
}
