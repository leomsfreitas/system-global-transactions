package com.ifsp.gcsw.transacoes.api.web;

import com.ifsp.gcsw.transacoes.api.web.dto.CotacaoResponse;
import com.ifsp.gcsw.transacoes.dominio.servico.ServicoCotacao;
import com.ifsp.gcsw.transacoes.dominio.taxa.TaxaResolvida;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cotacoes/{base}/{destino}")
public class CotacaoControlador {

  private static final String CODIGO = "^[A-Za-z]{3}$";
  private static final String MENSAGEM = "deve ser um codigo ISO 4217 de 3 letras";

  private final ServicoCotacao servico;

  public CotacaoControlador(ServicoCotacao servico) {
    this.servico = servico;
  }

  @GetMapping
  public ResponseEntity<CotacaoResponse> atual(
      @PathVariable @Pattern(regexp = CODIGO, message = MENSAGEM) String base,
      @PathVariable @Pattern(regexp = CODIGO, message = MENSAGEM) String destino) {
    TaxaResolvida taxa =
        servico.atual(base.toUpperCase(Locale.ROOT), destino.toUpperCase(Locale.ROOT));
    return ResponseEntity.ok()
        .header(Cabecalhos.DATA_REFERENCIA, Mapeador.dataReferencia(taxa.dataReferencia()))
        .body(Mapeador.cotacao(taxa));
  }

  @GetMapping("/historico")
  public List<CotacaoResponse> historico(
      @PathVariable @Pattern(regexp = CODIGO, message = MENSAGEM) String base,
      @PathVariable @Pattern(regexp = CODIGO, message = MENSAGEM) String destino,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
    return servico
        .historico(
            base.toUpperCase(Locale.ROOT), destino.toUpperCase(Locale.ROOT), dataInicio, dataFim)
        .stream()
        .map(Mapeador::cotacao)
        .toList();
  }
}
