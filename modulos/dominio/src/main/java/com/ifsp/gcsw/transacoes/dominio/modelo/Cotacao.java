package com.ifsp.gcsw.transacoes.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Immutable;

/**
 * Cotacao gravada pelo worker. Somente leitura no JPA: a escrita usa INSERT ... ON CONFLICT DO
 * NOTHING (historico nunca e sobrescrito).
 */
@Entity
@Immutable
@Table(name = "cotacao")
@IdClass(CotacaoId.class)
public class Cotacao {

  @Id private Long id;

  @Id
  @Column(name = "data_hora", nullable = false)
  private OffsetDateTime dataHora;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "moeda_base_id")
  private Moeda moedaBase;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "moeda_destino_id")
  private Moeda moedaDestino;

  @Column(nullable = false, precision = 18, scale = 6)
  private BigDecimal valor;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "fonte_id")
  private FonteDados fonte;

  protected Cotacao() {}

  public Long getId() {
    return id;
  }

  public OffsetDateTime getDataHora() {
    return dataHora;
  }

  public Moeda getMoedaBase() {
    return moedaBase;
  }

  public Moeda getMoedaDestino() {
    return moedaDestino;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public FonteDados getFonte() {
    return fonte;
  }
}
