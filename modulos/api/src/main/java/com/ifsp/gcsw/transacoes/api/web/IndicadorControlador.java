package com.ifsp.gcsw.transacoes.api.web;

import com.ifsp.gcsw.transacoes.api.web.dto.IndicadorEconomicoResponse;
import com.ifsp.gcsw.transacoes.dominio.servico.ServicoIndicador;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/indicadores")
public class IndicadorControlador {

  private final ServicoIndicador servico;

  public IndicadorControlador(ServicoIndicador servico) {
    this.servico = servico;
  }

  @GetMapping("/{pais}")
  public List<IndicadorEconomicoResponse> listar(
      @PathVariable
          @Pattern(regexp = "^[A-Za-z]{2}$", message = "deve ser um codigo ISO 3166 de 2 letras")
          String pais) {
    return servico.listar(pais.toUpperCase(Locale.ROOT)).stream().map(Mapeador::indicador).toList();
  }
}
