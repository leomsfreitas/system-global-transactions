package com.ifsp.gcsw.transacoes.conectores.bancomundial;

import com.ifsp.gcsw.transacoes.conectores.FonteRiscoPolitico;
import com.ifsp.gcsw.transacoes.conectores.modelo.IndicadorColetado;
import com.ifsp.gcsw.transacoes.conectores.modelo.PaisConsulta;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import java.util.List;

/** Adaptador: expoe a estabilidade politica do Banco Mundial como {@link FonteRiscoPolitico}. */
public class RiscoPoliticoBancoMundial implements FonteRiscoPolitico {

  private final BancoMundialCliente cliente;

  public RiscoPoliticoBancoMundial(BancoMundialCliente cliente) {
    this.cliente = cliente;
  }

  @Override
  public String codigo() {
    return BancoMundialCliente.CODIGO;
  }

  @Override
  public ResultadoColeta<IndicadorColetado> buscar(List<PaisConsulta> paises) {
    return cliente.estabilidadePolitica(paises);
  }
}
