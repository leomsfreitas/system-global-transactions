package com.ifsp.gcsw.transacoes.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ifsp.gcsw.transacoes.api.seguranca.FiltroLimiteRequisicoes;
import com.ifsp.gcsw.transacoes.api.seguranca.PropriedadesSeguranca;
import com.ifsp.gcsw.transacoes.api.web.Cabecalhos;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class FiltroLimiteRequisicoesTest {

  @Test
  void excedeOLimite_429ComRetryAfter() throws Exception {
    FiltroLimiteRequisicoes filtro =
        new FiltroLimiteRequisicoes(
            new PropriedadesSeguranca.LimiteRequisicoes(true, 1, 2), JsonMapper.builder().build());
    int[] status = new int[3];
    MockHttpServletResponse ultima = null;
    for (int i = 0; i < 3; i++) {
      MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/moedas");
      req.setAttribute(Cabecalhos.ATRIBUTO_PREFIXO_HASH, "abcd1234");
      ultima = new MockHttpServletResponse();
      filtro.doFilter(req, ultima, new MockFilterChain());
      status[i] = ultima.getStatus();
    }
    assertThat(status).containsExactly(200, 200, 429);
    assertThat(ultima.getHeader("Retry-After")).isEqualTo("1");
    assertThat(ultima.getContentAsString())
        .contains("\"codigo\":429")
        .contains("Limite de requisicoes excedido");
  }

  @Test
  void chavesDiferentesTemBaldesSeparados() throws Exception {
    FiltroLimiteRequisicoes filtro =
        new FiltroLimiteRequisicoes(
            new PropriedadesSeguranca.LimiteRequisicoes(true, 1, 1), JsonMapper.builder().build());
    for (String prefixo : new String[] {"aaaa1111", "bbbb2222"}) {
      MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/moedas");
      req.setAttribute(Cabecalhos.ATRIBUTO_PREFIXO_HASH, prefixo);
      MockHttpServletResponse resp = new MockHttpServletResponse();
      filtro.doFilter(req, resp, new MockFilterChain());
      assertThat(resp.getStatus()).isEqualTo(200);
    }
  }

  @Test
  void rotasForaDaApiNaoSaoLimitadas() throws Exception {
    FiltroLimiteRequisicoes filtro =
        new FiltroLimiteRequisicoes(
            new PropriedadesSeguranca.LimiteRequisicoes(true, 1, 1), JsonMapper.builder().build());
    for (int i = 0; i < 5; i++) {
      MockHttpServletResponse resp = new MockHttpServletResponse();
      filtro.doFilter(
          new MockHttpServletRequest("GET", "/actuator/health"), resp, new MockFilterChain());
      assertThat(resp.getStatus()).isEqualTo(200);
    }
  }
}
