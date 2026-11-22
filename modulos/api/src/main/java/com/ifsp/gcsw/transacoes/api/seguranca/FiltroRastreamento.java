package com.ifsp.gcsw.transacoes.api.seguranca;

import com.ifsp.gcsw.transacoes.api.web.Cabecalhos;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gera (ou reaproveita, se vier do Gateway e for valido) o traceId da requisicao, coloca no MDC
 * para os logs JSON e devolve no cabecalho X-Trace-Id.
 */
public class FiltroRastreamento extends OncePerRequestFilter {

  private static final Pattern TRACE_VALIDO = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

  @Override
  protected void doFilterInternal(
      HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
      throws ServletException, IOException {
    String recebido = requisicao.getHeader(Cabecalhos.TRACE_ID);
    String traceId =
        recebido != null && TRACE_VALIDO.matcher(recebido).matches()
            ? recebido
            : UUID.randomUUID().toString().replace("-", "");
    MDC.put("traceId", traceId);
    resposta.setHeader(Cabecalhos.TRACE_ID, traceId);
    try {
      cadeia.doFilter(requisicao, resposta);
    } finally {
      MDC.remove("traceId");
    }
  }
}
