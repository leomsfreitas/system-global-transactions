package com.ifsp.gcsw.transacoes.dominio.repositorio;

import com.ifsp.gcsw.transacoes.dominio.taxa.ConsultaCotacoes;
import com.ifsp.gcsw.transacoes.dominio.taxa.CotacaoVigente;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador da porta {@link ConsultaCotacoes} sobre o repositorio JPA. */
@Component
@Transactional(readOnly = true)
public class ConsultaCotacoesJpa implements ConsultaCotacoes {

  private final CotacaoRepositorio repositorio;

  public ConsultaCotacoesJpa(CotacaoRepositorio repositorio) {
    this.repositorio = repositorio;
  }

  @Override
  public Optional<CotacaoVigente> maisRecente(String base, String destino, String fonte) {
    return repositorio.maisRecente(base, destino, fonte).map(p -> converter(base, destino, p));
  }

  @Override
  public Optional<CotacaoVigente> ultimaDoDia(
      String base, String destino, String fonte, LocalDate dia) {
    return repositorio
        .ultimaNoIntervalo(base, destino, fonte, inicioDoDia(dia), inicioDoDia(dia.plusDays(1)))
        .map(p -> converter(base, destino, p));
  }

  @Override
  public List<CotacaoVigente> ultimasPorDia(
      String base, String destino, String fonte, LocalDate inicio, LocalDate fim) {
    return repositorio
        .ultimasPorDia(base, destino, fonte, inicioDoDia(inicio), inicioDoDia(fim.plusDays(1)))
        .stream()
        .map(p -> converter(base, destino, p))
        .toList();
  }

  private static Instant inicioDoDia(LocalDate dia) {
    return dia.atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  private static CotacaoVigente converter(String base, String destino, CotacaoProjecao p) {
    return new CotacaoVigente(
        base,
        destino,
        p.getValor(),
        p.getDataHora().atOffset(ZoneOffset.UTC),
        p.getFonte(),
        p.getFonteId());
  }
}
