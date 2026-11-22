package com.ifsp.gcsw.transacoes.api.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Cabecalhos de seguranca (secao 6.7, OWASP Secure Headers). */
public class FiltroCabecalhosSeguranca extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
      throws ServletException, IOException {
    resposta.setHeader("X-Content-Type-Options", "nosniff");
    resposta.setHeader("X-Frame-Options", "DENY");
    resposta.setHeader("Referrer-Policy", "no-referrer");
    resposta.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    resposta.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=()");
    resposta.setHeader("Cross-Origin-Resource-Policy", "same-origin");
    if (requisicao.getRequestURI().startsWith("/api/")) {
      resposta.setHeader("Cache-Control", "no-store");
      resposta.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
    }
    cadeia.doFilter(requisicao, resposta);
  }
}
