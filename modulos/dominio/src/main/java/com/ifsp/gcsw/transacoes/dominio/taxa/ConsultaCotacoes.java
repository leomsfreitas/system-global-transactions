package com.ifsp.gcsw.transacoes.dominio.taxa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Porta de leitura das cotacoes gravadas. fonte nula significa qualquer fonte. Dias em UTC. */
public interface ConsultaCotacoes {

  Optional<CotacaoVigente> maisRecente(String base, String destino, String fonte);

  Optional<CotacaoVigente> ultimaDoDia(String base, String destino, String fonte, LocalDate dia);

  /** A ultima cotacao de cada dia no intervalo [inicio, fim], em ordem cronologica. */
  List<CotacaoVigente> ultimasPorDia(
      String base, String destino, String fonte, LocalDate inicio, LocalDate fim);
}
