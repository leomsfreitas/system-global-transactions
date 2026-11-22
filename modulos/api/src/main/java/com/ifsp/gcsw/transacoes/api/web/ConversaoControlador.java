package com.ifsp.gcsw.transacoes.api.web;

import com.ifsp.gcsw.transacoes.api.web.dto.ConversaoRequest;
import com.ifsp.gcsw.transacoes.api.web.dto.ConversaoResponse;
import com.ifsp.gcsw.transacoes.dominio.servico.ConversaoRealizada;
import com.ifsp.gcsw.transacoes.dominio.servico.ServicoConversao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversoes")
public class ConversaoControlador {

  private final ServicoConversao servico;

  public ConversaoControlador(ServicoConversao servico) {
    this.servico = servico;
  }

  @PostMapping
  public ResponseEntity<ConversaoResponse> converter(
      @Valid @RequestBody ConversaoRequest requisicao, HttpServletRequest http) {
    ConversaoRealizada resultado =
        servico.converter(
            requisicao.moedaOrigem().toUpperCase(Locale.ROOT),
            requisicao.moedaDestino().toUpperCase(Locale.ROOT),
            requisicao.valor(),
            requisicao.ajusteAtivo(),
            (String) http.getAttribute(Cabecalhos.ATRIBUTO_PREFIXO_HASH));
    return ResponseEntity.ok()
        .header(Cabecalhos.DATA_REFERENCIA, Mapeador.dataReferencia(resultado.dataReferencia()))
        .body(Mapeador.conversao(resultado));
  }
}
