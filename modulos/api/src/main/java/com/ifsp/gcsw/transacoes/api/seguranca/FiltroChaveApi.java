package com.ifsp.gcsw.transacoes.api.seguranca;

import com.ifsp.gcsw.transacoes.api.web.Cabecalhos;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Valida X-API-Key contra a lista de hashes SHA-256 (defesa em profundidade; em producao o API
 * Gateway valida antes). Ausente ou invalida: 401. Guarda os 8 primeiros caracteres do hash para
 * auditoria e log; a chave em texto claro nunca e registrada.
 */
public class FiltroChaveApi extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(FiltroChaveApi.class);

  private final Set<String> hashesValidos;
  private final JsonMapper json;

  public FiltroChaveApi(PropriedadesSeguranca props, JsonMapper json) {
    this.hashesValidos =
        props.chavesApiSha256().stream()
            .map(String::trim)
            .filter(h -> !h.isEmpty())
            .map(h -> h.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    this.json = json;
    if (hashesValidos.isEmpty()) {
      LOG.warn(
          "Nenhuma chave de API configurada (seguranca.chaves-api-sha256): todas as requisicoes terao 401");
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest requisicao) {
    return !requisicao.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
      throws ServletException, IOException {
    String chave = requisicao.getHeader(Cabecalhos.CHAVE_API);
    String hash = chave == null || chave.isBlank() ? null : Hash.sha256(chave);
    if (hash == null || !hashesValidos.contains(hash)) {
      RespostaErro.escrever(
          resposta, json, HttpServletResponse.SC_UNAUTHORIZED, "Chave de API ausente ou invalida");
      return;
    }
    String prefixo = hash.substring(0, 8);
    requisicao.setAttribute(Cabecalhos.ATRIBUTO_PREFIXO_HASH, prefixo);
    MDC.put("chaveApiPrefixo", prefixo);
    try {
      cadeia.doFilter(requisicao, resposta);
    } finally {
      MDC.remove("chaveApiPrefixo");
    }
  }
}
