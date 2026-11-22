package com.ifsp.gcsw.transacoes.dominio.taxa;

import static com.ifsp.gcsw.transacoes.dominio.motor.Arredondamento.DIVISAO;
import static com.ifsp.gcsw.transacoes.dominio.motor.Arredondamento.ESCALA_TAXA;
import static com.ifsp.gcsw.transacoes.dominio.motor.Arredondamento.MODO;

import com.ifsp.gcsw.transacoes.dominio.excecao.DadoDesatualizadoException;
import com.ifsp.gcsw.transacoes.dominio.excecao.DadoIndisponivelException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Resolve a taxa de um par (secao 5.4, ADR 0009): par direto, depois inverso (1 / valor), depois
 * taxa cruzada por cada pivo configurado (BRL com dados do BCB, depois EUR com dados do BCE),
 * sempre com cotacoes do mesmo dia. Usa a primeira candidata que respeite a regra de frescor (secao
 * 5.6).
 *
 * <p>Sem Spring: depende so da porta {@link ConsultaCotacoes} e de um {@link Clock}.
 */
public final class ResolvedorTaxaCambio {

  private final ConsultaCotacoes consulta;
  private final List<Pivo> pivos;
  private final Duration idadeMaxima;
  private final Clock relogio;

  public ResolvedorTaxaCambio(
      ConsultaCotacoes consulta, List<Pivo> pivos, Duration idadeMaxima, Clock relogio) {
    this.consulta = consulta;
    this.pivos = List.copyOf(pivos);
    this.idadeMaxima = idadeMaxima;
    this.relogio = relogio;
  }

  public TaxaResolvida resolver(String base, String destino) {
    if (base.equals(destino)) {
      return new TaxaResolvida(
          base,
          destino,
          BigDecimal.ONE.setScale(ESCALA_TAXA),
          OffsetDateTime.now(relogio),
          "Identidade",
          null,
          MetodoTaxa.IDENTIDADE);
    }
    List<Supplier<Optional<TaxaResolvida>>> candidatas = new ArrayList<>();
    candidatas.add(() -> direta(base, destino));
    candidatas.add(() -> inversa(base, destino));
    for (Pivo pivo : pivos) {
      candidatas.add(() -> cruzada(base, destino, pivo));
    }

    TaxaResolvida maisRecenteDesatualizada = null;
    for (Supplier<Optional<TaxaResolvida>> candidata : candidatas) {
      Optional<TaxaResolvida> taxa = candidata.get();
      if (taxa.isEmpty()) {
        continue;
      }
      if (estaFresca(taxa.get().dataReferencia())) {
        return taxa.get();
      }
      if (maisRecenteDesatualizada == null
          || taxa.get().dataReferencia().isAfter(maisRecenteDesatualizada.dataReferencia())) {
        maisRecenteDesatualizada = taxa.get();
      }
    }
    if (maisRecenteDesatualizada != null) {
      throw desatualizada(base, destino, maisRecenteDesatualizada.dataReferencia());
    }
    throw new DadoIndisponivelException(
        "Nenhuma fonte publica cotacao para o par %s/%s".formatted(base, destino));
  }

  /** Verifica a regra de frescor sobre uma taxa ja resolvida (ex.: lida do cache). */
  public void exigirFrescor(TaxaResolvida taxa) {
    if (taxa.metodo() != MetodoTaxa.IDENTIDADE && !estaFresca(taxa.dataReferencia())) {
      throw desatualizada(taxa.base(), taxa.destino(), taxa.dataReferencia());
    }
  }

  public boolean estaFresca(OffsetDateTime dataReferencia) {
    return !dataReferencia.isBefore(OffsetDateTime.now(relogio).minus(idadeMaxima));
  }

  /**
   * Historico de um par, uma cotacao por dia (a ultima do dia), pelo mesmo encadeamento: direto,
   * inverso, cruzado. Retorna lista vazia se nenhum metodo tiver dados no periodo.
   */
  public List<TaxaResolvida> historico(
      String base, String destino, LocalDate inicio, LocalDate fim) {
    List<CotacaoVigente> diretas = consulta.ultimasPorDia(base, destino, null, inicio, fim);
    if (!diretas.isEmpty()) {
      return diretas.stream().map(c -> deCotacao(base, destino, c, MetodoTaxa.DIRETA)).toList();
    }
    List<CotacaoVigente> inversas = consulta.ultimasPorDia(destino, base, null, inicio, fim);
    if (!inversas.isEmpty()) {
      return inversas.stream()
          .map(c ->
              new TaxaResolvida(
                  base,
                  destino,
                  inverter(c.valor()),
                  c.dataHora(),
                  c.fonte(),
                  c.fonteId(),
                  MetodoTaxa.INVERSA))
          .toList();
    }
    for (Pivo pivo : pivos) {
      if (pivo.moeda().equals(base) || pivo.moeda().equals(destino)) {
        continue;
      }
      Map<LocalDate, Perna> pernasBase = pernasPorDia(base, pivo, inicio, fim);
      Map<LocalDate, Perna> pernasDestino = pernasPorDia(destino, pivo, inicio, fim);
      List<TaxaResolvida> cruzadas = new ArrayList<>();
      pernasBase.forEach((dia, pernaBase) -> {
        Perna pernaDestino = pernasDestino.get(dia);
        if (pernaDestino != null) {
          cruzadas.add(combinar(base, destino, pivo, pernaBase, pernaDestino));
        }
      });
      if (!cruzadas.isEmpty()) {
        return cruzadas;
      }
    }
    return List.of();
  }

  private Optional<TaxaResolvida> direta(String base, String destino) {
    return consulta
        .maisRecente(base, destino, null)
        .map(c -> deCotacao(base, destino, c, MetodoTaxa.DIRETA));
  }

  private Optional<TaxaResolvida> inversa(String base, String destino) {
    return consulta
        .maisRecente(destino, base, null)
        .map(c ->
            new TaxaResolvida(
                base,
                destino,
                inverter(c.valor()),
                c.dataHora(),
                c.fonte(),
                c.fonteId(),
                MetodoTaxa.INVERSA));
  }

  private Optional<TaxaResolvida> cruzada(String base, String destino, Pivo pivo) {
    if (pivo.moeda().equals(base) || pivo.moeda().equals(destino)) {
      return Optional.empty();
    }
    Optional<Perna> pernaBase = pernaMaisRecente(base, pivo);
    if (pernaBase.isPresent()) {
      Optional<Perna> pernaDestino = pernaNoDia(destino, pivo, dia(pernaBase.get().dataHora()));
      if (pernaDestino.isPresent()) {
        return Optional.of(combinar(base, destino, pivo, pernaBase.get(), pernaDestino.get()));
      }
    }
    Optional<Perna> pernaDestino = pernaMaisRecente(destino, pivo);
    if (pernaDestino.isPresent()) {
      Optional<Perna> pernaBaseNoDia = pernaNoDia(base, pivo, dia(pernaDestino.get().dataHora()));
      if (pernaBaseNoDia.isPresent()) {
        return Optional.of(combinar(base, destino, pivo, pernaBaseNoDia.get(), pernaDestino.get()));
      }
    }
    return Optional.empty();
  }

  /** base -> destino = (base -> pivo) / (destino -> pivo). */
  private TaxaResolvida combinar(
      String base, String destino, Pivo pivo, Perna pernaBase, Perna pernaDestino) {
    BigDecimal taxa =
        pernaBase
            .taxaParaPivo()
            .divide(pernaDestino.taxaParaPivo(), DIVISAO)
            .setScale(ESCALA_TAXA, MODO);
    OffsetDateTime referencia =
        pernaBase.dataHora().isBefore(pernaDestino.dataHora())
            ? pernaBase.dataHora()
            : pernaDestino.dataHora();
    return new TaxaResolvida(
        base,
        destino,
        taxa,
        referencia,
        "Cruzada: " + pivo.fonte(),
        pernaBase.fonteId(),
        MetodoTaxa.CRUZADA);
  }

  /** Taxa moeda -> pivo, a partir do par gravado moeda->pivo ou pivo->moeda, o mais recente. */
  private Optional<Perna> pernaMaisRecente(String moeda, Pivo pivo) {
    Optional<Perna> ida = consulta.maisRecente(moeda, pivo.moeda(), pivo.fonte()).map(Perna::ida);
    Optional<Perna> volta =
        consulta.maisRecente(pivo.moeda(), moeda, pivo.fonte()).map(Perna::volta);
    return maisRecente(ida, volta);
  }

  private Optional<Perna> pernaNoDia(String moeda, Pivo pivo, LocalDate dia) {
    Optional<Perna> ida =
        consulta.ultimaDoDia(moeda, pivo.moeda(), pivo.fonte(), dia).map(Perna::ida);
    Optional<Perna> volta =
        consulta.ultimaDoDia(pivo.moeda(), moeda, pivo.fonte(), dia).map(Perna::volta);
    return maisRecente(ida, volta);
  }

  private Map<LocalDate, Perna> pernasPorDia(
      String moeda, Pivo pivo, LocalDate inicio, LocalDate fim) {
    Map<LocalDate, Perna> porDia = new TreeMap<>();
    for (CotacaoVigente c :
        consulta.ultimasPorDia(pivo.moeda(), moeda, pivo.fonte(), inicio, fim)) {
      porDia.put(dia(c.dataHora()), Perna.volta(c));
    }
    for (CotacaoVigente c :
        consulta.ultimasPorDia(moeda, pivo.moeda(), pivo.fonte(), inicio, fim)) {
      porDia.merge(
          dia(c.dataHora()), Perna.ida(c), (a, b) -> a.dataHora().isAfter(b.dataHora()) ? a : b);
    }
    return porDia;
  }

  private static Optional<Perna> maisRecente(Optional<Perna> a, Optional<Perna> b) {
    return java.util.stream.Stream.of(a, b)
        .flatMap(Optional::stream)
        .max(Comparator.comparing(Perna::dataHora));
  }

  private static TaxaResolvida deCotacao(
      String base, String destino, CotacaoVigente c, MetodoTaxa metodo) {
    return new TaxaResolvida(
        base,
        destino,
        c.valor().setScale(ESCALA_TAXA, MODO),
        c.dataHora(),
        c.fonte(),
        c.fonteId(),
        metodo);
  }

  private static BigDecimal inverter(BigDecimal valor) {
    return BigDecimal.ONE.divide(valor, DIVISAO).setScale(ESCALA_TAXA, MODO);
  }

  private static LocalDate dia(OffsetDateTime dataHora) {
    return dataHora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
  }

  private DadoDesatualizadoException desatualizada(
      String base, String destino, OffsetDateTime referencia) {
    return new DadoDesatualizadoException(
        "A cotacao mais recente de %s/%s e de %s, mais antiga que o limite de %d dias"
            .formatted(base, destino, referencia.toLocalDate(), idadeMaxima.toDays()),
        referencia);
  }

  /** Uma perna da taxa cruzada: quanto 1 unidade da moeda vale no pivo (precisao cheia). */
  private record Perna(BigDecimal taxaParaPivo, OffsetDateTime dataHora, Long fonteId) {
    static Perna ida(CotacaoVigente c) {
      return new Perna(c.valor(), c.dataHora(), c.fonteId());
    }

    static Perna volta(CotacaoVigente c) {
      return new Perna(BigDecimal.ONE.divide(c.valor(), DIVISAO), c.dataHora(), c.fonteId());
    }
  }
}
