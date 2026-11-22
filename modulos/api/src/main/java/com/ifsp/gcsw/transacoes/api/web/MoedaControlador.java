package com.ifsp.gcsw.transacoes.api.web;

import com.ifsp.gcsw.transacoes.api.web.dto.MoedaResponse;
import com.ifsp.gcsw.transacoes.dominio.servico.ServicoMoeda;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/moedas")
public class MoedaControlador {

  private final ServicoMoeda servico;

  public MoedaControlador(ServicoMoeda servico) {
    this.servico = servico;
  }

  @GetMapping
  public List<MoedaResponse> listar() {
    return servico.listar().stream().map(Mapeador::moeda).toList();
  }
}
