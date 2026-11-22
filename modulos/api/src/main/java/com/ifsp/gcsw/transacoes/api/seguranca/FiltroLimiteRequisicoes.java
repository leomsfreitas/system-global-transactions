package com.ifsp.gcsw.transacoes.api.seguranca;

import com.ifsp.gcsw.transacoes.api.web.Cabecalhos;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Limite de requisicoes por chave com Bucket4j (secao 5.7). Ligado no perfil local; em producao o
 * usage plan do API Gateway faz esse papel. Excedido: 429 com Retry-After.
 */
public class FiltroLimiteRequisicoes extends OncePerRequestFilter {

  private final PropriedadesSeguranca.LimiteRequisicoes limite;
  private final JsonMapper json;
  private final ConcurrentHashMap<String, Bucket> baldes = new ConcurrentHashMap<>();

  public FiltroLimiteRequisicoes(PropriedadesSeguranca.LimiteRequisicoes limite, JsonMapper json) {
    this.limite = limite;
    this.json = json;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest requisicao) {
    return !requisicao.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
      throws ServletException, IOException {
    Object prefixo = requisicao.getAttribute(Cabecalhos.ATRIBUTO_PREFIXO_HASH);
    String chave = prefixo == null ? "anonimo" : prefixo.toString();
    ConsumptionProbe sonda =
        baldes.computeIfAbsent(chave, k -> novoBalde()).tryConsumeAndReturnRemaining(1);
    if (!sonda.isConsumed()) {
      // Arredonda para cima, minimo de 1 segundo.
      long segundos =
          Math.max(1, (sonda.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
      resposta.setHeader("Retry-After", Long.toString(segundos));
      RespostaErro.escrever(resposta, json, 429, "Limite de requisicoes excedido");
      return;
    }
    cadeia.doFilter(requisicao, resposta);
  }

  private Bucket novoBalde() {
    return Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(limite.rajada())
                .refillGreedy(limite.requisicoesPorSegundo(), Duration.ofSeconds(1))
                .build())
        .build();
  }
}
