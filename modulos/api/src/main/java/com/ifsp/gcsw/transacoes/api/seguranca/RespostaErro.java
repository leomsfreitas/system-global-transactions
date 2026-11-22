package com.ifsp.gcsw.transacoes.api.seguranca;

import com.ifsp.gcsw.transacoes.api.web.dto.Erro;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/** Escreve o corpo Erro direto na resposta (filtros rodam antes do RestControllerAdvice). */
final class RespostaErro {

  private RespostaErro() {}

  static void escrever(HttpServletResponse resposta, JsonMapper json, int status, String mensagem)
      throws IOException {
    resposta.setStatus(status);
    resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
    resposta.setCharacterEncoding("UTF-8");
    resposta.getWriter().write(json.writeValueAsString(Erro.de(status, mensagem)));
  }
}
