package com.ifsp.gcsw.transacoes.api.web;

import com.ifsp.gcsw.transacoes.api.web.dto.IndiceGeopoliticoResponse;
import com.ifsp.gcsw.transacoes.dominio.servico.ServicoIndiceGeopolitico;
import jakarta.validation.constraints.Pattern;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/indices-geopoliticos")
public class IndiceGeopoliticoControlador {

  private final ServicoIndiceGeopolitico servico;

  public IndiceGeopoliticoControlador(ServicoIndiceGeopolitico servico) {
    this.servico = servico;
  }

  @GetMapping("/{pais}")
  public IndiceGeopoliticoResponse atual(
      @PathVariable
          @Pattern(regexp = "^[A-Za-z]{2}$", message = "deve ser um codigo ISO 3166 de 2 letras")
          String pais) {
    return Mapeador.indice(servico.atual(pais.toUpperCase(Locale.ROOT)));
  }
}
