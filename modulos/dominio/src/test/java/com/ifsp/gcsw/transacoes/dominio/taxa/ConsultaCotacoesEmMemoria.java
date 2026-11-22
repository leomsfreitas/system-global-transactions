package com.ifsp.gcsw.transacoes.dominio.taxa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** Implementacao em memoria da porta, para testar o resolvedor sem banco. */
class ConsultaCotacoesEmMemoria implements ConsultaCotacoes {

  private final List<CotacaoVigente> cotacoes = new ArrayList<>();

  ConsultaCotacoesEmMemoria com(
      String base, String destino, String valor, OffsetDateTime dataHora, String fonte) {
    cotacoes.add(
        new CotacaoVigente(
            base, destino, new BigDecimal(valor), dataHora, fonte, (long) fonte.hashCode()));
    return this;
  }

  private Stream<CotacaoVigente> filtrar(String base, String destino, String fonte) {
    return cotacoes.stream()
        .filter(c -> c.base().equals(base) && c.destino().equals(destino))
        .filter(c -> fonte == null || c.fonte().equals(fonte));
  }

  @Override
  public Optional<CotacaoVigente> maisRecente(String base, String destino, String fonte) {
    return filtrar(base, destino, fonte).max(Comparator.comparing(CotacaoVigente::dataHora));
  }

  @Override
  public Optional<CotacaoVigente> ultimaDoDia(
      String base, String destino, String fonte, LocalDate dia) {
    return filtrar(base, destino, fonte)
        .filter(c -> dia(c).equals(dia))
        .max(Comparator.comparing(CotacaoVigente::dataHora));
  }

  @Override
  public List<CotacaoVigente> ultimasPorDia(
      String base, String destino, String fonte, LocalDate inicio, LocalDate fim) {
    Map<LocalDate, CotacaoVigente> porDia = new LinkedHashMap<>();
    filtrar(base, destino, fonte)
        .filter(c -> !dia(c).isBefore(inicio) && !dia(c).isAfter(fim))
        .sorted(Comparator.comparing(CotacaoVigente::dataHora))
        .forEach(c -> porDia.put(dia(c), c));
    return List.copyOf(porDia.values());
  }

  private static LocalDate dia(CotacaoVigente c) {
    return c.dataHora().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
  }
}
